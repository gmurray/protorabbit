package org.protorabbit.accelerator;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.protorabbit.Config;
import org.protorabbit.IOUtil;
import org.protorabbit.model.IContext;
import org.protorabbit.model.ITemplate;
import org.protorabbit.model.ResourceURI;

/**
 * Manage Combined Resources
 * 
 * @author Greg Murray
 * 
 */
public class CombinedResourceManager {

    private String resourceService;
    
    private Config cfg = null;

    private Hashtable<String, CacheableResource> combinedResources = null; 

    // in milliseconds
    private long maxTimeout;


    /**
     * @param ctx
     */
    public CombinedResourceManager (
    		Config cfg,
            String resourceService,
            long maxTimeout ) {
    	
    	this.cfg = cfg;
        this.resourceService = resourceService;
        this.maxTimeout = maxTimeout;
        combinedResources = new Hashtable<String, CacheableResource>();

    }

    public String getResourceService() {
        return resourceService;
    }

    // TODO : create a flush for unused resources
    
    /*
     * This code replaces relative CSS links in a CSS file to abolute paths 
     * based on the widgetDir 
     *
     */
    private StringBuffer replaceRelativeLinks(StringBuffer buffer, String widgetDir) {
        int index = 0;
        while (true) {
            int start = buffer.indexOf("url(", index);
            // TODO : also get url(" and url('
            // around for the "url(" portion if not -1
            if (start == -1 ) break;
            else start += 4;
            if (start  > buffer.length())break;
            // find the end of the URL
            int end = buffer.indexOf(")",start );
            if (end == -1) break;
            String url = buffer.substring(start,end);
            if (!url.startsWith("http")) {
                url = widgetDir + "/"+ url; 
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
        

    
    public CacheableResource getScripts(List<ResourceURI>scriptResources, IContext ctx) throws IOException {

        CacheableResource scripts = new CacheableResource("text/javascript", maxTimeout, getHash(scriptResources));
        
        Iterator<ResourceURI> it = scriptResources.iterator();
        while (it.hasNext()) {
            ResourceURI ri = it.next();
      
            StringBuffer scriptBuffer = ctx.getResource(ri.getBaseURI(), ri.getUri());
            try {
                scripts.appendContent(scriptBuffer.toString());
            } catch (Exception ioe) {
               System.out.println("Unable to locate resource "  +ri.getUri());
            }
        }
        return scripts;
    }

    public CacheableResource getStyles(List<ResourceURI>styleResources, IContext ctx) throws IOException {

        CacheableResource styles = new CacheableResource("text/css", maxTimeout, getHash(styleResources));
        
        Iterator<ResourceURI> it = styleResources.iterator();
        while (it.hasNext()) {
            ResourceURI ri = it.next();
            StringBuffer stylesBuffer = ctx.getResource(ri.getBaseURI(), ri.getUri());
            try {
                stylesBuffer = replaceRelativeLinks(stylesBuffer, ri.getBaseURI());
                styles.appendContent(stylesBuffer.toString());
            } catch (Exception ioe) {
            	System.out.println("Unable to locate resource "  +ri.getUri());
            }
        }
        return styles;
    }
    
    public CacheableResource getResource(String key) {
    	CacheableResource csr = combinedResources.get(key);
    	if (csr != null) {
    		return csr;
    	}
    	return null;
    }
    
    public String processStyles(List<ResourceURI>styleResources,  IContext ctx, OutputStream out) throws java.io.IOException {

        CacheableResource csr;
        String hash = getHash(styleResources);
        if (hash != null) {

            if (combinedResources.get(hash) != null) {
                csr = combinedResources.get(hash);
                
                if (csr.getCacheContext().isExpired()) {
                    csr.reset();
                    csr = getStyles(styleResources,ctx);
                }
            } else {
                csr = getStyles(styleResources,ctx);
            }
			boolean gzip = true;
			ITemplate t = ctx.getConfig().getTemplate(ctx.getTemplateId());
			gzip = t.gzipStyles();
			csr.setGzipResources(gzip);            
            combinedResources.put(hash, csr);
            String uri = "<link rel=\"stylesheet\" type=\"text/css\" href=\"" +
                         getResourceService() + "?id=" + hash + 
                         ".css\" media=\"" + cfg.mediaType() + "\" />";
            
            out.write(uri.getBytes());

        }
        return hash;
    }
    
    public String processScripts(List<ResourceURI>scriptResources, IContext ctx, OutputStream out) throws java.io.IOException {
    	
    	if (scriptResources.size() == 0 ) {
    		return null;
    	}

    	CacheableResource csr;
		String hash = getHash(scriptResources);

		if (combinedResources.get(hash) != null) {
			
			csr = combinedResources.get(hash);

			if (csr.getCacheContext().isExpired()) {
				csr.reset();
				csr = getScripts(scriptResources,ctx);
			}
		} else {
			csr = getScripts(scriptResources,ctx);
		}
		boolean gzip = true;
		ITemplate t = ctx.getConfig().getTemplate(ctx.getTemplateId());
		gzip = t.gzipScripts();
		csr.setGzipResources(gzip);		
		combinedResources.put(hash, csr);
		String uri = "<script src=\"" + 
		             getResourceService() + "?id=" + hash + 
				     ".js\"></script>";
		out.write(uri.getBytes());
		return hash;
	}
}

