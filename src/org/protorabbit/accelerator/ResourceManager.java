/*
 * Protorabbit
 *
 * Copyright (c) 2009 Greg Murray (protorabbit.org)
 * 
 * Licensed under the MIT License:
 * 
 *  http://www.opensource.org/licenses/mit-license.php
 *
 */

package org.protorabbit.accelerator;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.protorabbit.Config;
import org.protorabbit.accelerator.impl.CacheableResource;
import org.protorabbit.model.IContext;
import org.protorabbit.model.ITemplate;
import org.protorabbit.model.impl.IncludeCommand;
import org.protorabbit.model.impl.ResourceURI;
import org.protorabbit.util.IOUtil;

/**
 * Manage Combined Resources
 * 
 * @author Greg Murray
 * 
 */
public class ResourceManager {

    private String resourceService;
    private Hashtable<String, ICacheable> combinedResources = null;

    // in milliseconds
    private long maxTimeout;

    /**
     *  CombinedResourceManager - Responsible for combined CSS and Script resources.
     */
    public ResourceManager (
            Config cfg,
            String resourceService,
            long maxTimeout ) {

        this.resourceService = resourceService;
        this.maxTimeout = maxTimeout;
        combinedResources = new Hashtable<String, ICacheable>();

    }

    /*
     * 
     * Reset resources that have exceeded their max timeout and
     * remove objects that have exceeded the threshhold.
     * 
     */
    public void cleanup(long threshhold) {
        Iterator<String> it = combinedResources.keySet().iterator();
        long now = (new Date()).getTime();
        while (it.hasNext()){
            String key = it.next();
            ICacheable c = combinedResources.get(key);
            long diff = c.getLastAccessed() - now;
            // don't delete it if it is loading
            if (c.isLoaded() &&
                (diff > c.getTimeout() ||
                diff > threshhold)) {
                
                if (diff > threshhold) {
                	combinedResources.remove(key);
                } else {
                	c.reset();
                }
            }
        }
    }

    public String getResourceService() {
        return resourceService;
    }

    /*
     * This code replaces relative CSS links in a CSS file to absolute paths
     * based on the relative CSS file location.
     * 
     * This should also handle remapping of private resources under the /WEB-INF dir
     * to non app structure revealing names.
     *
     */
    private StringBuffer replaceRelativeLinks(StringBuffer buffer, String resourceDir, IContext ctx, String resourceName) {
        int index = 0;
        while (true) {
            int start = buffer.indexOf("url(", index);
            // TODO : also get url(" and url('
            // around for the "url(" portion if not -1
            if (start == -1 ) break;
            else start += 4;
            if (start > buffer.length()) break;
            // find the end of the URL
            int end = buffer.indexOf(")", start );
            if (end == -1) break;
            // The raw contents of what is between the url()
            String url = buffer.substring(start,end);
            url = url.trim();
            // trim leading / trailing quotes
            if (url.startsWith("\"") || url.startsWith("\'")) {
                url = url.substring(1);
            }
            if (url.endsWith("\"") || url.endsWith("\'")) {
                url = url.substring(url.length() -1);
            }
            // don't replace externalized resources
            if (!url.startsWith("http")) {
                if ( resourceDir.startsWith("/WEB-INF") && !url.startsWith("/")) {
                    Config.getLogger().warning("Non Fatal error replacing style references. Reference to url "  + url +
                                            " in " + resourceName + " is located in a private directory '/WEB-INF'. " + 
                                            " Place the resource in an accesible location or use a non relative link or place" +
                                            " the css template in a public directory.");
                } else {
                    // make sure "/" resources are mapped to the context root
                    if (url.startsWith("/")) {
                        url = "\"" + ctx.getContextRoot() + url + "\"";
                    } else {
                        url = "\"" + ctx.getContextRoot() + resourceDir + url + "\"";
                    }
                }
            }
            buffer.replace(start,end, url);
            index = start + url.length() + 1;
        }
        return buffer;
    }

    /**
     * Calculate the MD5 based hash based on a sorted list of the of the
     * styles or scripts
     * 
     * @return MD5 Hash or null
     */
    public String getHash(List<ResourceURI> uriResources) {

        Collections.sort(uriResources, new ResourceURIComparator());
            
            Iterator<ResourceURI> it = uriResources.iterator();
            String namesString = "";
            while (it.hasNext()) {
                namesString += "" + it.next().getFullURI();
            }
            return IOUtil.generateHash(namesString);

    }  
    
    class ResourceURIComparator implements Comparator<ResourceURI> {

        public int compare(ResourceURI o1, ResourceURI o2) {
            String uri1 = o1.getFullURI();
            String uri2 = o2.getFullURI();
            return uri1.compareTo(uri2);
        }
    }

    @SuppressWarnings("unchecked")
    public CacheableResource getScripts(List<ResourceURI>scriptResources, IContext ctx, OutputStream out) throws IOException {

        CacheableResource scripts = new CacheableResource("text/javascript", maxTimeout, getHash(scriptResources));

        List<String> deferredScripts = (List<String>)ctx.getAttribute(IncludeCommand.DEFERRED_SCRIPTS);

        Iterator<ResourceURI> it = scriptResources.iterator();
        while (it.hasNext()) {
            ResourceURI ri = it.next();
            if (ri.isWritten()) continue;
            String resource = ri.getUri();
            String baseURI =  ctx.getContextRoot();

            if (!ri.isExternal()){
                // map to root
                if (resource.startsWith("/")) {
                    baseURI = ctx.getContextRoot();
                } else {
                   baseURI +=  ri.getBaseURI();
                }
            } else {
                baseURI = "";
            }
            if (ri.isDefer()) {
                if (deferredScripts == null) {
                    deferredScripts = new ArrayList<String>();
                }
                String fragement = "<script>protorabbit.addDeferredScript('" +
                                   baseURI + resource + "');</script>";
                deferredScripts.add(fragement);
                ri.setWritten(true);
                ctx.setAttribute(IncludeCommand.DEFERRED_SCRIPTS, deferredScripts);
            } else if (!ri.isExternal()){
                StringBuffer scriptBuffer = ctx.getResource(ri.getBaseURI(), ri.getUri());
                try {
                    scripts.appendContent(scriptBuffer.toString());
                    ri.updateLastUpdated(ctx);
                } catch (Exception ioe) {
                   System.out.println("Unable to locate resource " + ri.getUri());
                }
            } else {
                String script = "<script type=\"text/javascript\" src=\"" +
                                resource + "\"></script>";
                out.write(script.getBytes());
                ri.setWritten(true);
            }
        }
        return scripts;
    }

    @SuppressWarnings("unchecked")
    public CacheableResource getStyles(List<ResourceURI>styleResources, IContext ctx, OutputStream out) throws IOException {

        CacheableResource styles = new CacheableResource("text/css", maxTimeout, getHash(styleResources));
        List<String> deferredScripts = (List<String>)ctx.getAttribute(IncludeCommand.DEFERRED_SCRIPTS);
        Iterator<ResourceURI> it = styleResources.iterator();
        while (it.hasNext()) {
            ResourceURI ri = it.next();
            if (ri.isWritten()) continue;
            String mediaType = ri.getMediaType();
            String resource = ri.getUri();
            String baseURI =  ctx.getContextRoot();

            if (!ri.isExternal()){
                // map to root
                if (resource.startsWith("/")) {
                    baseURI = ctx.getContextRoot();
                } else {
                   baseURI +=  ri.getBaseURI();
                }
            } else {
                baseURI = "";
            }
            if (mediaType == null){
                mediaType = ctx.getConfig().mediaType();
            }
            if (ri.isDefer()) {
                if (deferredScripts == null) {
                    deferredScripts = new ArrayList<String>();
                }
                String fragement = "<script>protorabbit.addDeferredStyle('" +
                                    baseURI + resource + "','" + mediaType + "');</script>";
                deferredScripts.add(fragement);
                ri.setWritten(true);
                ctx.setAttribute(IncludeCommand.DEFERRED_SCRIPTS, deferredScripts);
            } else if (!ri.isExternal()){
                StringBuffer stylesBuffer = ctx.getResource(ri.getBaseURI(), ri.getUri());
                try {
                    stylesBuffer = replaceRelativeLinks(stylesBuffer, ri.getBaseURI(), ctx, ri.getFullURI());
                    styles.appendContent(stylesBuffer.toString());
                    ri.updateLastUpdated(ctx);
                } catch (Exception ioe) {
                    Config.getLogger().warning("Non Fatal Error : Unable to locate resource "  +ri.getUri());
                }
            } else {
                String uri = "<link rel=\"stylesheet\" type=\"text/css\" href=\"" +
                    resource + "\"/>";
                 out.write(uri.getBytes());
                 ri.setWritten(true);
            }
        }
        return styles;
    }

    public ICacheable getResource(String key) {
        ICacheable csr = combinedResources.get(key);
        if (csr != null) {
            return csr;
        }
        return null;
    }

    public void putResource(String key, ICacheable csr) {
        combinedResources.put(key, csr);
    }

    public String processStyles(List<ResourceURI>styleResources,
                                IContext ctx,
                                OutputStream out) throws java.io.IOException {

        ICacheable csr;
        String hash = getHash(styleResources);
        // check if any of the combined resources are expired if dev mode
        if (ctx.getConfig().getDevMode()) {
            boolean requiresRefresh = false;
            for (ResourceURI item : styleResources) {
                if (item.isUpdated(ctx)) {
                    requiresRefresh = true;
                    break;
                }
            }
            // remove the hash
            if (requiresRefresh && hash != null) {
                combinedResources.remove(hash);
            }
        }

        if (combinedResources.get(hash) != null) {
            csr = combinedResources.get(hash);

            if (csr.getCacheContext().isExpired()) {
                csr.reset();
                csr = getStyles(styleResources,ctx, out);
            }
        } else {
            csr = getStyles(styleResources,ctx, out);
        }
        boolean gzip = false;
        ITemplate t = ctx.getConfig().getTemplate(ctx.getTemplateId());
        if (t != null) {
            if (t.gzipStyles() != null) {
                gzip = t.gzipStyles();
            } else {
                gzip = ctx.getConfig().getGzip();
            }
            csr.setGzipResources(gzip);

            combinedResources.put(hash, csr);
           return hash;
        } else {
            return null;
        }

    }

    public String processScripts(List<ResourceURI>scriptResources,
                                 IContext ctx, boolean defer, OutputStream out) throws java.io.IOException {

        if (scriptResources.size() == 0 ) {
            return null;
        }

        ICacheable csr;
        String hash = getHash(scriptResources);
        // check if any of the combined resources are expired if dev mode
        if (ctx.getConfig().getDevMode()) {
            boolean requiresRefresh = false;
            for (ResourceURI item : scriptResources) {
                if (item.isUpdated(ctx)) {
                    requiresRefresh = true;
                    break;
                }
            }
            // remove the hash
            if (requiresRefresh && hash != null) {
                combinedResources.remove(hash);
            }
        }
        if (combinedResources.get(hash) != null) {

            csr = combinedResources.get(hash);

            if (csr.getCacheContext().isExpired()) {
                csr.reset();
                csr = getScripts(scriptResources,ctx, out);
            }
        } else {
            csr = getScripts(scriptResources,ctx, out);
        }

        ITemplate t = ctx.getConfig().getTemplate(ctx.getTemplateId());
        if (t != null) {

            boolean gzip = false;

            if (t.gzipScripts() == null) {
                gzip = ctx.getConfig().getGzip();
            } else {
                gzip = t.gzipScripts();
            }
            csr.setGzipResources(gzip);
            combinedResources.put(hash, csr);
            return hash;
        } else {
            return null;
        }
    }
}

