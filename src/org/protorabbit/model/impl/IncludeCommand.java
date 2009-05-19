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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.protorabbit.Config;
import org.protorabbit.accelerator.ResourceManager;
import org.protorabbit.accelerator.impl.CacheableResource;
import org.protorabbit.accelerator.impl.DeferredResource;
import org.protorabbit.model.IParameter;
import org.protorabbit.model.IProperty;
import org.protorabbit.model.ITemplate;
import org.protorabbit.util.IOUtil;

public class IncludeCommand extends BaseCommand {

    public static final String COUNTER = "COUNTER";
    public static final String DEFERRED_SCRIPTS = "DEFERRED_SCRIPTS";

    private static Logger logger = null;

    public static final Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("org.protrabbit");
        }
        return logger;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void doProcess(OutputStream out) throws IOException {

        Config cfg = ctx.getConfig();
        int counter = 0;
        if (ctx.getAttribute(COUNTER) != null) {
            counter = ((Integer)ctx.getAttribute(COUNTER)).intValue();
        } 
        boolean useThreadedDefer = true;

        String tid = ctx.getTemplateId();

        String id = null;
        if (params.length > 0 && params[0].getType() == IParameter.STRING) {
            id = params[0].getValue().toString();
        } else {
            getLogger().severe("Error processing property " + params[0].getValue().toString() + " Parameter is not of type String");
            return;
        }

        String resourceName = null;
        String baseDir = null;
        IProperty property = null;

        ITemplate template = cfg.getTemplate(tid);
        if (template != null) {

            property = template.getProperty(id,ctx);

            if (property == null) {
                getLogger().log(Level.SEVERE, "Unable to find include file for " + id + " in template " + tid);
                return;
            }
            if (property.getUATest() != null) {
                if (ctx.uaTest(property.getUATest()) == false) {
                    return;
                }
            }
            if (property.getTest() != null) {
                if (ctx.test(property.getTest()) == false) {
                    return;
                }
            }
            resourceName = property.getValue();

            baseDir = "";
            if (!resourceName.startsWith("/") && !resourceName.startsWith("http")) {
                baseDir = property.getBaseURI();
            }

        }

        StringBuffer buff = new StringBuffer("");
        StringBuffer deferContent = new StringBuffer("");
        if (property.getDefer()){
            List<String> deferredScripts = (List<String>)ctx.getAttribute(DEFERRED_SCRIPTS);
            if (deferredScripts == null) {
                deferredScripts = new ArrayList<String>();
                ctx.setAttribute(DEFERRED_SCRIPTS, deferredScripts);
            }
            String hash = "";
            String resourceId = "";

            if (useThreadedDefer) {
                hash  = IOUtil.generateHash(baseDir + resourceName);
                resourceId = hash;
                ResourceManager crm = cfg.getCombinedResourceManager();
                Long timeout = property.getTimeout();
                if (timeout == null) {
                    timeout = 0L;
                }
                DeferredResource dr = new DeferredResource(baseDir, resourceName, ctx, timeout);
                crm.putResource(resourceId, dr);
            } else {
                IncludeFile inc = cfg.getIncludeFileContent(ctx.getTemplateId(), id,ctx);
                buff = inc.getContent();
                hash = IOUtil.generateHash(buff.toString());
                resourceId = hash;
                if (inc.getDeferContent() != null) {
                    deferContent = inc.getDeferContent();
                }
                ResourceManager crm = cfg.getCombinedResourceManager();
                CacheableResource cr = new CacheableResource("text/html", inc.getTimeout(), hash);
                cr.setContent( buff );
                crm.putResource(resourceId, cr);
            }

            out.write(("<div id='" + resourceId + "'>" + deferContent.toString() + "</div>").getBytes());

            String script = "<script>protorabbit.addDeferredFragement({ include : '" + cfg.getResourceService() + 
                       "?resourceid=" + resourceId + ".htm', elementId : '" + resourceId + "' });</script>";
            deferredScripts.add(script);
            ctx.setAttribute(COUNTER, new Integer(counter + 1));
        } else {
            IncludeFile inc = cfg.getIncludeFileContent(ctx.getTemplateId(), id,ctx);
            buff = inc.getContent();
            out.write(buff.toString().getBytes());
        }

    }
}