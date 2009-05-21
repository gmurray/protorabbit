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
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.protorabbit.Config;
import org.protorabbit.accelerator.ResourceManager;
import org.protorabbit.accelerator.ICacheable;
import org.protorabbit.accelerator.impl.CacheableResource;
import org.protorabbit.json.JSONSerializer;
import org.protorabbit.json.SerializationFactory;
import org.protorabbit.model.ICommand;
import org.protorabbit.model.IParameter;
import org.protorabbit.model.ITemplate;
import org.protorabbit.util.IOUtil;

public class IncludeResourcesCommand extends BaseCommand {

    public static String DEFERRED_WRITTEN = "deferredWritten";

    private String protorabbitClient = "resources/protorabbit.js";

    private static Logger logger = null;

    protected int commandType = ICommand.INCLUDE_RESOURCES;

    public IncludeResourcesCommand(){
        super();
        // set this command to process after everything else
        setProcessOrder(ICommand.PROCESS_LAST);
    }

    public static final Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("org.protrabbit");
        }
        return logger;
    }

    private void writeDeferred(Config cfg, OutputStream out, ITemplate t) throws IOException {
        StringBuffer buff = IOUtil.getClasspathResource(cfg, protorabbitClient);
        if (buff != null) {
            String hash = IOUtil.generateHash(buff.toString());
            ICacheable cr = new CacheableResource("text/javascript", t.getTimeout(), hash);
            cfg.getCombinedResourceManager().putResource("protorabbit", cr);
            cr.setContent(buff);
            String uri = "<script src=\"" + 
            cfg.getResourceService() + "?resourceid=protorabbit.js\"></script>";
            out.write(uri.getBytes());
            ctx.setAttribute(DEFERRED_WRITTEN, Boolean.TRUE);
        } else {
            getLogger().severe("Unable to find protorabbit client script");
        }
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

        boolean deferredWritten = (ctx.getAttribute(DEFERRED_WRITTEN) != null &&
                ctx.getAttribute(DEFERRED_WRITTEN) == Boolean.TRUE);

        if ("scripts".equals(target)) {
            List<ResourceURI> scripts = t.getAllScripts(ctx);
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
                    writeDeferred(cfg, buffer, t);
                }
           }
           if (t.getCombineScripts() != null && t.getCombineScripts()) {

                String hash = crm.processScripts(scripts, ctx, hasDeferredScripts, buffer);

                if (!hasDeferredScripts && hash != null) {
                    String uri = "<script src=\"" + 
                    cfg.getResourceService() + "?resourceid=" + hash +  ".js\"></script>";
                    buffer.write(uri.getBytes());

               } else if (hash != null){
                   buffer.write(("<script>protorabbit.addDeferredScript('" + cfg.getResourceService() +
                              "?resourceid=" + hash + ".js');</script>").getBytes());
               }

           } else {
                String tFile = cfg.getResourceReferences(ctx.getTemplateId(), target, ctx);
                buffer.write(tFile.getBytes());
           }
           if (deferredScripts != null) {
               for (String s : deferredScripts) {
                   buffer.write(s.getBytes());
               }
           }
           if (deferredProperties != null) {
               SerializationFactory factory = new SerializationFactory();
               JSONSerializer js = factory.getInstance();
               JSONObject jo = (JSONObject)js.serialize(deferredProperties);
               String content = jo.toString();
               String hash = IOUtil.generateHash(content);
               CacheableResource cr = new CacheableResource("text/html", cfg.getResourceTimeout(), hash);
               cr.setContent( new StringBuffer(content) );
               crm.putResource(hash, cr);
               buffer.write(("<script>protorabbit.addDeferredProperties('" + cfg.getResourceService() +
                       "?resourceid=" + hash + ".json', '" + ctx.getTemplateId() +"');</script>").getBytes());
           }
        } else if ("styles".equals(target)) {
            List<ResourceURI> styles = t.getAllStyles(ctx);
            boolean hasDeferredStyles = false;

            if (!deferredWritten) {

                for (ResourceURI r : styles) {
                    if (r.isDefer()) {
                        hasDeferredStyles = true;
                    }
                }

                if (hasDeferredStyles) {
                    writeDeferred(cfg,buffer, t);
                }
            }

            if (t.getCombineStyles() != null && t.getCombineStyles()) {
                String hash = crm.processStyles(styles, ctx, buffer);
                if (!hasDeferredStyles && hash != null) {
                    String uri = "<link rel=\"stylesheet\" type=\"text/css\" href=\"" +
                    cfg.getResourceService() + "?resourceid=" + hash + 
                    ".css\"/>";
                    buffer.write(uri.getBytes());
               } else if (hash != null){
                   String uri = "<script>protorabbit.addDeferredStyle('" + 
                   cfg.getResourceService() + "?resourceid=" + hash + ".css')</script>";
                   buffer.write(uri.getBytes());
               }
            } else {
                String uri = cfg.getResourceReferences(ctx.getTemplateId(), target, ctx);
                buffer.write(uri.getBytes());
            }
        }

    }
}
