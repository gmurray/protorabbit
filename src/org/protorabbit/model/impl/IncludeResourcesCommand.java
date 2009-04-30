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
import org.protorabbit.IOUtil;
import org.protorabbit.accelerator.CombinedResourceManager;
import org.protorabbit.accelerator.ICacheable;
import org.protorabbit.accelerator.impl.CacheableResource;
import org.protorabbit.model.ITemplate;

public class IncludeResourcesCommand extends BaseCommand {

    public static String DEFERRED_WRITTEN = "deferredWritten";
    
    private String protorabbitClient = "resources/protorabbit.js";

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
        List<String> deferredScripts = (List<String>)ctx.getAttribute(IncludeCommand.DEFERRED_SCRIPTS);
        if ("scripts".equals(target)) {

            boolean hasDeferred = false;

            List<ResourceURI> scripts = t.getAllScripts(ctx);
            
            for (ResourceURI r : scripts) {
                if (r.isDefer()) {
                    hasDeferred = true;
                }
            }

            boolean deferredWritten = (ctx.getAttribute(DEFERRED_WRITTEN) != null &&
                    ctx.getAttribute(DEFERRED_WRITTEN) == Boolean.TRUE);

            if ((hasDeferred  || deferredScripts != null ) && !deferredWritten) {
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
            if (t.getCombineScripts() != null && t.getCombineScripts()) {

                String hash = crm.processScripts(scripts, ctx, hasDeferred);

                if (!hasDeferred) {
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

        } else if ("styles".equals(target)) {
            if (t.getCombineStyles() != null && t.getCombineStyles()) {
                List<ResourceURI> styles = t.getAllStyles(ctx);
                crm.processStyles(styles, ctx, out);
            } else {
                String tFile = cfg.getResourceReferences(ctx.getTemplateId(), params[0], ctx);
                out.write(tFile.getBytes());
            }
        }

        if (deferredScripts != null) {
            for (String s : deferredScripts) {
                out.write(s.getBytes());
            }
        }
    }
}
