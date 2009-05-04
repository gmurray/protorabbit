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

import org.protorabbit.Config;
import org.protorabbit.accelerator.CombinedResourceManager;
import org.protorabbit.accelerator.ICacheable;
import org.protorabbit.accelerator.impl.CacheableResource;
import org.protorabbit.model.ITemplate;
import org.protorabbit.util.IOUtil;

public class IncludeResourcesCommand extends BaseCommand {

    public static String DEFERRED_WRITTEN = "deferredWritten";
    
    private String protorabbitClient = "resources/protorabbit.js";

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
            Config.getLogger().severe("Unable to find protorabbit client script");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void doProcess(OutputStream out) throws IOException {
        if (params == null || params.length < 1) {
            Config.getLogger().warning("Warning: IncludeReferences called with no parameter.");
            return;
        }

        Config cfg = ctx.getConfig();
        String target = params[0].toLowerCase();

        ITemplate t = cfg.getTemplate(ctx.getTemplateId());
        CombinedResourceManager crm = cfg.getCombinedResourceManager();

        boolean deferredWritten = (ctx.getAttribute(DEFERRED_WRITTEN) != null &&
                ctx.getAttribute(DEFERRED_WRITTEN) == Boolean.TRUE);

        if ("scripts".equals(target)) {

            List<ResourceURI> scripts = t.getAllScripts(ctx);
            List<String> deferredScripts = (List<String>)ctx.getAttribute(IncludeCommand.DEFERRED_SCRIPTS);
            boolean hasDeferredScripts = false;

            if (!deferredWritten) {

                for (ResourceURI r : scripts) {
                    if (r.isDefer()) {
                        hasDeferredScripts = true;
                    }
                }

                if ((hasDeferredScripts  || deferredScripts != null )) {
                    writeDeferred(cfg, out, t);
                }
           }
           if (t.getCombineScripts() != null && t.getCombineScripts()) {

                String hash = crm.processScripts(scripts, ctx, hasDeferredScripts);

                if (!hasDeferredScripts && hash != null) {
                    String uri = "<script src=\"" + 
                    cfg.getResourceService() + "?resourceid=" + hash +  ".js\"></script>";
                    out.write(uri.getBytes());

               } else if (hash != null){
                   out.write(("<script>protorabbit.addDeferredScript('" + cfg.getResourceService() +
                              "?resourceid=" + hash + ".js');</script>").getBytes());
               }

            } else {
                String tFile = cfg.getResourceReferences(ctx.getTemplateId(), params[0], ctx);
                out.write(tFile.getBytes());
            }
           if (deferredScripts != null) {
               for (String s : deferredScripts) {
                   out.write(s.getBytes());
               }
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
                    writeDeferred(cfg,out, t);
                }
            }
            String hash = crm.processStyles(styles, ctx, out);

            if (t.getCombineStyles() != null && t.getCombineStyles()) {
                if (!hasDeferredStyles && hash != null) {
                    String uri = "<link rel=\"stylesheet\" type=\"text/css\" href=\"" +
                    cfg.getResourceService() + "?resourceid=" + hash + 
                    ".css\"/>";
                     out.write(uri.getBytes());
               } else if (hash != null){
                   String uri = "<script>protorabbit.addDeferredStyle('" + 
                   cfg.getResourceService() + "?resourceid=" + hash +  ".css')</script>";
                   out.write(uri.getBytes());
               }
            } else {
                String uri = cfg.getResourceReferences(ctx.getTemplateId(), params[0], ctx);
                out.write(uri.getBytes());
            }
        }

    }
}
