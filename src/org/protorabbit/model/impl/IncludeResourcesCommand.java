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

package org.protorabbit.model.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.protorabbit.Config;
import org.protorabbit.accelerator.ResourceManager;
import org.protorabbit.accelerator.impl.CacheableResource;
import org.protorabbit.json.JSONSerializer;
import org.protorabbit.json.SerializationFactory;
import org.protorabbit.model.ICommand;
import org.protorabbit.model.IParameter;
import org.protorabbit.model.ITemplate;

public class IncludeResourcesCommand extends BaseCommand {

    private static Logger logger = null;

    protected int commandType = ICommand.INCLUDE_RESOURCES;

    public IncludeResourcesCommand(){
        super();
        // set this command to process after everything else
        setProcessOrder(ICommand.PROCESS_LAST);
    }

    static final Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("org.protrabbit");
        }
        return logger;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void doProcess() throws IOException {
        if (params == null || params.length < 1) {
            getLogger().warning("Warning: IncludeReferences called with no parameter.");
            return;
        }

        Config cfg = ctx.getConfig();
        String target = null;
        if (params.length > 0 && params[0].getType() == IParameter.STRING) {
            target = params[0].getValue().toString();
        } else {
            getLogger().severe("Error processing property " + params[0].getValue().toString() + " Parameter is not of type String");
            return;
        }
        target = target.toLowerCase();
        ITemplate t = cfg.getTemplate(ctx.getTemplateId());
        ResourceManager crm = cfg.getCombinedResourceManager();

        boolean deferredWritten = (ctx.getAttribute(ResourceManager.DEFERRED_WRITTEN) != null &&
                ctx.getAttribute(ResourceManager.DEFERRED_WRITTEN) == Boolean.TRUE);

        if ("scripts".equals(target)) {

            List<ResourceURI> scripts = t.getAllScripts(ctx);
            List<ResourceURI> combineURIs= new ArrayList<ResourceURI>();
            List<ResourceURI> linkURIs = new ArrayList<ResourceURI>();

            for (ResourceURI ri : scripts) {
                if (ri.getCombine() != null){
                    if (ri.getCombine().booleanValue() == true) {
                        combineURIs.add(ri);
                    } else {
                        linkURIs.add(ri);
                    }
                } else {
                    if ( t.getCombineScripts() != null &&
                         t.getCombineScripts().booleanValue() == true) {
                        combineURIs.add(ri);
                    } else {
                        linkURIs.add(ri);
                    }
                }

            }

            List<String> deferredScripts = (List<String>)ctx.getAttribute(IncludeCommand.DEFERRED_SCRIPTS);
            Map<String, String> deferredProperties = (Map<String, String>)ctx.getAttribute(InsertCommand.DEFERRED_PROPERTIES);

            boolean hasDeferredScripts = false;

            if (!deferredWritten) {

                for (ResourceURI r : scripts) {
                    if (r.isDefer()) {
                        hasDeferredScripts = true;
                    }
                }

                if ((hasDeferredScripts  || deferredScripts != null || deferredProperties != null)) {
                    ResourceManager.writeDeferred(ctx, buffer, t);
                }
           }
           if (linkURIs.size() > 0 ){
                String links = URIResourceManager.generateReferences(t, ctx, linkURIs, ResourceURI.SCRIPT);
                buffer.write(links.getBytes());
           }
           if (combineURIs.size() > 0) {

                String resourceId = crm.processScripts(combineURIs, ctx, hasDeferredScripts, buffer);

                if (!hasDeferredScripts && resourceId != null) {
                    if (ctx.getConfig().profile()) {
                         String measure =  "<script>" +
                             "window.postMessage(\"EPISODES:mark:" + resourceId + "\", \"*\");" +
                         "</script>\n";
                         buffer.write(measure.getBytes());
                    }
                    String uri = "<script src=\"" + 
                    cfg.getResourceService() + "?resourceid=" + resourceId +  ".js&tid=" + t.getId() + "\"></script>";
                    buffer.write(uri.getBytes());
                    if (ctx.getConfig().profile()) {
                        String measure =  "<script>" +
                            "window.postMessage(\"EPISODES:measure:" + resourceId + "\", \"*\");" +
                        "</script>\n";
                        buffer.write(measure.getBytes());
                    }
               } else if (resourceId != null){
                   buffer.write(("<script>protorabbit.addDeferredScript('" + cfg.getResourceService() +
                              "?resourceid=" + resourceId + ".js&tid=" + t.getId() + "');</script>").getBytes());
               }

           }
           if (deferredScripts != null) {

               if (!deferredWritten) {
                   ResourceManager.writeDeferred(ctx, buffer, t);
               }
               for (String s : deferredScripts) {
                   buffer.write(s.getBytes());
               }
           }
           if (deferredProperties != null) {
               SerializationFactory factory = new SerializationFactory();
               JSONSerializer js = factory.getInstance();
               JSONObject jo = (JSONObject)js.serialize(deferredProperties);
               String content = jo.toString();
               String resourceId = "messages";
               CacheableResource cr = new CacheableResource("application/json", cfg.getResourceTimeout(), resourceId);
               cr.setContent( new StringBuffer(content) );
               crm.putResource(t.getId() + "_" + resourceId, cr);
               buffer.write(("<script>protorabbit.addDeferredProperties('" + cfg.getResourceService() +
                       "?resourceid=" + resourceId + ".json&tid=" + t.getId() + "', '" + ctx.getTemplateId() +"');</script>").getBytes());
           }
        } else if ("styles".equals(target)) {
            List<ResourceURI> styles = t.getAllStyles(ctx);
            boolean hasDeferredStyles = false;
            List<ResourceURI> combineURIs= new ArrayList<ResourceURI>();
            List<ResourceURI> linkURIs = new ArrayList<ResourceURI>();

            for (ResourceURI ri : styles) {
                if (ri.getCombine() != null){
                    if (ri.getCombine().booleanValue() == true) {
                        combineURIs.add(ri);
                    } else {
                        linkURIs.add(ri);
                    }
                } else {
                    if ( t.getCombineStyles() != null &&
                         t.getCombineStyles().booleanValue() == true) {
                        combineURIs.add(ri);
                    } else {
                        linkURIs.add(ri);
                    }
                }

            }
            if (linkURIs.size() > 0) {
                String links = URIResourceManager.generateReferences(t, ctx, linkURIs, ResourceURI.LINK);
                buffer.write(links.getBytes());
            }
            if (combineURIs.size() > 0) {
                String resourceId = crm.processStyles(styles, ctx, buffer);
                if (!hasDeferredStyles && resourceId != null) {

                     if (ctx.getConfig().profile()) {
                         String measure =  "<script>" +
                             "window.postMessage(\"EPISODES:mark:" + resourceId + "\", \"*\");" +
                         "</script>\n";
                         buffer.write(measure.getBytes());
                     }

                    String uri = "<link rel=\"stylesheet\" type=\"text/css\" href=\"" +
                    cfg.getResourceService() + "?resourceid=" + resourceId + 
                    ".css&tid=" + t.getId() + "\" media=\"" + cfg.getMediaType() + "\"/>";
                    buffer.write(uri.getBytes());
                    if (ctx.getConfig().profile()) {
                        String measure =  "<script>" +
                            "window.postMessage(\"EPISODES:measure:" + resourceId + "\", \"*\");" +
                        "</script>\n";
                        buffer.write(measure.getBytes());
                   }
                    
               } else if (resourceId != null){
                   String uri = "<script>protorabbit.addDeferredStyle('" + 
                   cfg.getResourceService() + "?resourceid=" + resourceId + ".css&tid=" + t.getId() + "')</script>";
                   buffer.write(uri.getBytes());
               }
            }
            if (!deferredWritten) {

                for (ResourceURI r : styles) {
                    if (r.isDefer()) {
                        hasDeferredStyles = true;
                    }
                }

                if (hasDeferredStyles) {
                    ResourceManager.writeDeferred(ctx,buffer, t);
                }
            }
            if (ctx.getConfig().profile()) {
                if (!deferredWritten) {
                    ResourceManager.writeDeferred(ctx, buffer, t);
                }

                // skip the default processing if the property is not set
                if (ctx.getAttribute(Config.DEFAULT_EPISODE_PROCESS) == null) {
                     String episodeEnd = "<script>window.episodesDefaultLoad = false;</script>";
                     buffer.write(episodeEnd.getBytes());
                }

            }

        }

    }
}