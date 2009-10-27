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

        Config cfg = getContext().getConfig();
        String target = null;
        if (params.length > 0 && params[0].getType() == IParameter.STRING) {
            target = params[0].getValue().toString();
        } else {
            getLogger().severe("Error processing property " + params[0].getValue().toString() + " Parameter is not of type String");
            return;
        }
        target = target.toLowerCase();
        ITemplate t = cfg.getTemplate(getContext().getTemplateId());
        ResourceManager crm = cfg.getCombinedResourceManager();

        boolean deferredWritten = (getContext().getAttribute(ResourceManager.DEFERRED_WRITTEN) != null &&
                getContext().getAttribute(ResourceManager.DEFERRED_WRITTEN) == Boolean.TRUE);

        if ("scripts".equals(target)) {

            List<ResourceURI> scripts = t.getAllScripts(getContext());
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

            List<String> deferredScripts = (List<String>)getContext().getAttribute(IncludeCommand.DEFERRED_SCRIPTS);
            Map<String, String> deferredProperties = (Map<String, String>)getContext().getAttribute(InsertCommand.DEFERRED_PROPERTIES);

            boolean hasDeferredScripts = false;

            if (!deferredWritten) {

                for (ResourceURI r : scripts) {
                    if (r.isDefer()) {
                        hasDeferredScripts = true;
                    }
                }

           }
           if (linkURIs.size() > 0 ){
                String links = URIResourceManager.generateReferences(t, getContext(), linkURIs, ResourceURI.SCRIPT);
                getBuffer().write(links.getBytes());
           }
           if (combineURIs.size() > 0) {

                String resourceId = crm.processScripts(combineURIs, getContext(), hasDeferredScripts, getBuffer());

                if (!hasDeferredScripts && resourceId != null) {
                    if (getContext().getConfig().profile()) {
                         String measure =  "<script>" +
                             "window.postMessage(\"EPISODES:mark:" + resourceId + "\", \"*\");" +
                         "</script>\n";
                         getBuffer().write(measure.getBytes());
                    }
                    String uuid = "";
                    if (t.getUniqueURL() != null && t.getUniqueURL() == Boolean.TRUE) {
                        uuid = "&puuid" + t.getCreateTime();
                    }
                    String uri = "<script src=\"" + 
                    cfg.getResourceService() + "?resourceid=" + resourceId +  ".js&tid=" + t.getId() + uuid + "\"></script>";
                    getBuffer().write(uri.getBytes());
                    if (getContext().getConfig().profile()) {
                        String measure =  "<script>" +
                            "window.postMessage(\"EPISODES:measure:" + resourceId + "\", \"*\");" +
                        "</script>\n";
                        getBuffer().write(measure.getBytes());
                    }
               } else if (resourceId != null){
                   ResourceManager.writeDeferred(getContext(), getBuffer(), t);
                   getBuffer().write(("<script>protorabbit.addDeferredScript('" + cfg.getResourceService() +
                              "?resourceid=" + resourceId + ".js&tid=" + t.getId() + "');</script>").getBytes());
               }

           }
           if (deferredScripts != null) {
               ResourceManager.writeDeferred(getContext(), getBuffer(), t);
               for (String s : deferredScripts) {
                   getBuffer().write(s.getBytes());
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
               ResourceManager.writeDeferred(getContext(), getBuffer(), t);
               getBuffer().write(("<script>protorabbit.addDeferredProperties('" + cfg.getResourceService() +
                       "?resourceid=" + resourceId + ".json&tid=" + t.getId() + "', '" + getContext().getTemplateId() +"');</script>").getBytes());
           }
        } else if ("styles".equals(target)) {
            List<ResourceURI> styles = t.getAllStyles(getContext());
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
                String links = URIResourceManager.generateReferences(t, getContext(), linkURIs, ResourceURI.LINK);
                getBuffer().write(links.getBytes());
            }
            if (combineURIs.size() > 0) {
                String resourceId = crm.processStyles(styles, getContext(), getBuffer());
                if (!hasDeferredStyles && resourceId != null) {

                     if (getContext().getConfig().profile()) {
                         String measure =  "<script>" +
                             "window.postMessage(\"EPISODES:mark:" + resourceId + "\", \"*\");" +
                         "</script>\n";
                         getBuffer().write(measure.getBytes());
                     }
                     String uuid = "";
                     if (t.getUniqueURL() != null && t.getUniqueURL() == Boolean.TRUE) {
                         uuid = "&puuid=" + t.getCreateTime();
                     }
                    String uri = "<link rel=\"stylesheet\" type=\"text/css\" href=\"" +
                    cfg.getResourceService() + "?resourceid=" + resourceId + 
                    ".css&tid=" + t.getId() + uuid + "\" media=\"" + cfg.getMediaType() + "\"/>";
                    getBuffer().write(uri.getBytes());
                    if (getContext().getConfig().profile()) {
                        String measure =  "<script>" +
                            "window.postMessage(\"EPISODES:measure:" + resourceId + "\", \"*\");" +
                        "</script>\n";
                        getBuffer().write(measure.getBytes());
                   }
                    
               } else if (resourceId != null){
                   ResourceManager.writeDeferred(getContext(), getBuffer(), t);
                   String uri = "<script>protorabbit.addDeferredStyle('" + 
                   cfg.getResourceService() + "?resourceid=" + resourceId + ".css&tid=" + t.getId() + "')</script>";
                   getBuffer().write(uri.getBytes());
               }
            }
            if (!deferredWritten) {

                for (ResourceURI r : styles) {
                    if (r.isDefer()) {
                        hasDeferredStyles = true;
                    }
                }

                if (hasDeferredStyles) {
                    ResourceManager.writeDeferred(getContext(),getBuffer(), t);
                }
            }
            if (getContext().getConfig().profile()) {
                if (!deferredWritten) {
                    ResourceManager.writeDeferred(getContext(), getBuffer(), t);
                }

                // skip the default processing if the property is not set
                if (getContext().getAttribute(Config.DEFAULT_EPISODE_PROCESS) == null) {
                     String episodeEnd = "<script>window.episodesDefaultLoad = false;</script>";
                     getBuffer().write(episodeEnd.getBytes());
                }

            }

        }

    }
}