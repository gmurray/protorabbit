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
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.protorabbit.Config;
import org.protorabbit.accelerator.impl.CacheableResource;
import org.protorabbit.model.IContext;
import org.protorabbit.model.ITemplate;
import org.protorabbit.model.impl.IncludeCommand;
import org.protorabbit.model.impl.ResourceURI;
import org.protorabbit.model.impl.Template;
import org.protorabbit.util.IOUtil;

/**
* Manage Combined Resources
*
* @author Greg Murray
*
*/
public class ResourceManager {

    public static String DEFERRED_WRITTEN = "deferredWritten";

   private String resourceService;
   private Hashtable<String, ICacheable> combinedResources = null;
   private static Logger logger = null;

   static final Logger getLogger() {
       if (logger == null) {
           logger = Logger.getLogger("org.protrabbit");
       }
       return logger;
   }

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

   public Map<String, ICacheable> getResources() {
       return combinedResources;
   }

   public static void writeDeferred(IContext ctx, OutputStream out, ITemplate t) throws IOException {
       boolean deferredWritten = false;
       if (ctx.getAttribute(DEFERRED_WRITTEN) != null) {
           deferredWritten = ((Boolean)ctx.getAttribute(DEFERRED_WRITTEN)).booleanValue();
       }
       if (!deferredWritten) {
           StringBuffer buff = IOUtil.getClasspathResource(ctx.getConfig(), Config.PROTORABBIT_CLIENT);
           if (buff != null) {
               String hash = IOUtil.generateHash(buff.toString());
               ICacheable cr = new CacheableResource("text/javascript", t.getTimeout(), hash);
               ctx.getConfig().getCombinedResourceManager().putResource("protorabbit", cr);
               cr.setContent(buff);
               String uri = "<script src=\"" + 
               ctx.getConfig().getResourceService() + "?resourceid=protorabbit.js\"></script>";
               out.write(uri.getBytes());
               ctx.setAttribute(DEFERRED_WRITTEN, Boolean.TRUE);
           } else {
               getLogger().severe("Unable to find protorabbit client script " + Config.PROTORABBIT_CLIENT);
           }
       }
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
           long timeout = 0;
           if (c.getTimeout() != null) {
               timeout = c.getTimeout().longValue();
           }
           if (c.isLoaded() &&
               (diff > timeout ||
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
           // handle cases where resource dirs not specified 
           if ("".equals(resourceDir) ) {
               int lastSep = resourceName.lastIndexOf("/");
               if (lastSep != -1) {
                   resourceDir = resourceName.substring(0, lastSep + 1);
               } 
           }
           // don't replace externalized resources
           if (!url.startsWith("http")) {
               if ( resourceDir.startsWith("/WEB-INF") && !url.startsWith("/")) {
                   getLogger().warning("Non Fatal error replacing style references. Reference to url "  + url +
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

           Iterator<ResourceURI> it = uriResources.iterator();
           String namesString = "";
           while (it.hasNext()) {
               namesString += "" + it.next().getFullURI();
           }
           return IOUtil.generateHash(namesString);

   }

   @SuppressWarnings("unchecked")
   public void getScripts(ICacheable scripts, List<ResourceURI>scriptResources, IContext ctx, OutputStream out) throws IOException {

           scripts.setTimeout(maxTimeout);
           scripts.setHash(getHash(scriptResources));


       List<String> deferredScripts = (List<String>)ctx.getAttribute(IncludeCommand.DEFERRED_SCRIPTS);
       ITemplate t = ctx.getConfig().getTemplate(ctx.getTemplateId());
       Iterator<ResourceURI> it = scriptResources.iterator();
       while (it.hasNext()) {
           ResourceURI ri = it.next();
           if (ri.isWritten()) continue;
           String resource = ri.getURI(t.getUniqueURL());
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
               StringBuffer scriptBuffer = ctx.getResource(ri.getBaseURI(), ri.getURI(t.getUniqueURL()));
               try {
                   scripts.appendContent(scriptBuffer.toString());
                   ri.updateLastUpdated(ctx);
               } catch (Exception ioe) {
                  getLogger().warning("Unable to locate resource " + ri.getURI(null));
               }
           } else {
               if (ctx.getConfig().profile()) {
                   String measure =  "<script>" +
                       "window.postMessage(\"EPISODES:mark:" + resource + "\", \"*\");" +
                   "</script>\n";
                   out.write(measure.getBytes());
               }
               String script = "<script type=\"text/javascript\" src=\"" +
                               resource + "\"></script>";
               out.write(script.getBytes());
               ri.setWritten(true);
               if (ctx.getConfig().profile()) {
                   String measure =  "<script>" +
                       "window.postMessage(\"EPISODES:measure:" + resource + "\", \"*\");" +
                   "</script>\n";
                   out.write(measure.getBytes());
              }
           }
       }

   }

   @SuppressWarnings("unchecked")
   public ICacheable getStyles(ICacheable styles, List<ResourceURI>styleResources, IContext ctx, OutputStream out) throws IOException {
       if (styles == null) {
           styles = new CacheableResource("text/css", maxTimeout, getHash(styleResources));
       } else {
           styles.setTimeout(maxTimeout);
           styles.setHash(getHash(styleResources));
       }
       List<String> deferredScripts = (List<String>)ctx.getAttribute(IncludeCommand.DEFERRED_SCRIPTS);
       Iterator<ResourceURI> it = styleResources.iterator();
       ITemplate t = ctx.getConfig().getTemplate(ctx.getTemplateId());
       while (it.hasNext()) {
           ResourceURI ri = it.next();
           if (ri.isWritten()) continue;
           String mediaType = ri.getMediaType();
           String resource = ri.getURI(t.getUniqueURL());
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
               mediaType = ctx.getConfig().getMediaType();
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
               StringBuffer stylesBuffer = ctx.getResource(ri.getBaseURI(), ri.getURI(null));
               try {
                   stylesBuffer = replaceRelativeLinks(stylesBuffer, ri.getBaseURI(), ctx, ri.getFullURI());
                   styles.appendContent(stylesBuffer.toString());
                   ri.updateLastUpdated(ctx);
               } catch (Exception ioe) {
                   getLogger().warning("Non Fatal Error : Unable to locate resource "  +ri.getURI(null));
               }
           } else {
               if (ctx.getConfig().profile()) {
                   String measure =  "<script>" +
                       "window.postMessage(\"EPISODES:mark:" + resource + "\", \"*\");" +
                   "</script>\n";
                   out.write(measure.getBytes());
              }
               String uri = "<link rel=\"stylesheet\" type=\"text/css\" href=\"" +
                   resource + "\"/>";
                out.write(uri.getBytes());
                ri.setWritten(true);
                if (ctx.getConfig().profile()) {
                    String measure =  "<script>" +
                        "window.postMessage(\"EPISODES:measure:" + resource + "\", \"*\");" +
                    "</script>\n";
                    out.write(measure.getBytes());
               }
           }
       }
       return styles;
   }

   public ICacheable getResource(String key, IContext ctx) {

       String ua = null;
       ITemplate t = ctx.getConfig().getTemplate(ctx.getTemplateId());
       ICacheable csr = combinedResources.get(key);

       boolean hasUATest = false;
       if (t != null && csr != null && csr.getResourceType() == Config.SCRIPT) {
           hasUATest = t.hasUserAgentScriptDependencies(ctx);
           if (hasUATest) {
               String uaTest = ctx.getUAScriptTests().get(0);
               if (ctx.uaTest(uaTest)) {
                   ua = uaTest;
               }
           }
       } else if (t != null && csr != null && csr.getResourceType() == Config.STYLE) {
           hasUATest = t.hasUserAgentScriptDependencies(ctx);
           if (hasUATest) {
               String uaTest = ctx.getUAScriptTests().get(0);
               if (ctx.uaTest(uaTest)) {
                   ua = uaTest;
               }
           }
       }

       if (csr != null) {
           // if there is a child resource for the given user agent return it
           // otherwise return the default resource.
           if (ua != null) {
               ICacheable child = csr.getResourceForUserAgent(ua);
               if (child != null) {
                   return child;
               } else {
                   return csr;
               }
           } else {
               return csr;
           }
       }
       return null;
   }

   public void putResource(String key, ICacheable csr) {
     putResource(key, csr, null);
   }

   public void putResource(String key, ICacheable csr, String userAgent) {
       ICacheable parent = combinedResources.get(key);

       if (userAgent != null) {

           if (parent == null) {
               parent = new CacheableResource(csr.getContentType(), null, null);
               combinedResources.put(key, parent);
           }
           if (parent != csr) {
               parent.addUserAgentResource(userAgent, csr);
               
           } else {
              getLogger().severe("Error adding template to cache: " + key + " can't add self for " + userAgent);
           }
       } else {
           if (!combinedResources.containsKey(key)) {
               combinedResources.put(key, csr);
           }
       }
   }

   public String processStyles(List<ResourceURI>styleResources,
                               IContext ctx,
                               OutputStream out) throws java.io.IOException {

       return processResources(styleResources,
                               ctx,
                               false,
                               Config.STYLE,
                               out);
   }

   public String processResources(List<ResourceURI>uriResources,
                                IContext ctx, boolean defer, int type, OutputStream out) throws java.io.IOException {

       if (uriResources.size() == 0 ) {
           return null;
       }
       ITemplate t = ctx.getConfig().getTemplate(ctx.getTemplateId());
       ICacheable csr = null;
       if (t == null) {
           return null;
       }
       String resourceId = null;

       boolean gzip = false;
       // only set the test if this template passes
       String test = null;

       if (Config.SCRIPT == type) {
           resourceId = "scripts";

           if (t.gzipScripts() == null) {
               gzip = ctx.getConfig().getGzip();
           } else {
               gzip = t.gzipScripts();
           }
           if (ctx.getUAScriptTests() != null && ctx.getUAScriptTests().size() == 1 ) {
               String testt = ctx.getUAScriptTests().get(0);
               if (ctx.uaTest(testt)) {
                   test = testt;
               }
           }
       } else if (Config.STYLE == type) {
           resourceId = "styles";

           if (t.gzipStyles() != null) {
               gzip = t.gzipStyles();
           } else {
               gzip = ctx.getConfig().getGzip();
           }
           if (ctx.getUAStyleTests() != null && ctx.getUAStyleTests().size() == 1 ) {
               String testt = ctx.getUAStyleTests().get(0);
               if (ctx.uaTest(testt)) {
                   test = testt;
               }
           }
       }

       // check if any of the combined resources are expired if dev mode
       boolean requiresRefresh = false;
       if (ctx.getConfig().getDevMode()) {
           for (ResourceURI item : uriResources) {
               if (item.isUpdated(ctx)) {
                   requiresRefresh = true;
                   break;
               }
           }
       }

       ICacheable targetResource = combinedResources.get(t.getId() + "_" +resourceId);

       if ((targetResource != null  ) || (targetResource != null && 
           (targetResource.getResourceForUserAgent(test) != null)) ) {
           // we have a resource matching the test return it

           if (test != null) {
               csr = targetResource.getResourceForUserAgent(test);
           }  else {
               csr = targetResource;
           }
           if (csr != null) {

                if (csr.getCacheContext().isExpired() || requiresRefresh) {
                    csr.reset();
                    if (Config.SCRIPT == type) {
                        getScripts(csr, uriResources, ctx, out);
                    } else if (Config.STYLE == type) {
                        getStyles(csr, uriResources, ctx, out);
                    }
                }
            }
       }
       if (csr == null) {

           if (Config.SCRIPT == type) {
               csr = new CacheableResource("text/javascript", null, null);
               getScripts(csr,uriResources,ctx, out);
           } else if (Config.STYLE == type) {
               csr = new CacheableResource("text/css", null, null);
               getStyles(csr, uriResources,ctx, out);
           }
           csr.setGzipResources(gzip);
          
       }
       putResource(t.getId() + "_" + resourceId, csr, test);
       return resourceId;
   }

   public String processScripts(List<ResourceURI>scriptResources,
           IContext ctx, boolean defer, OutputStream out) throws java.io.IOException {
       return processResources(scriptResources,
               ctx,
               defer,
               Config.SCRIPT,
               out);
   }

}
