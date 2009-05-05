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

import org.protorabbit.Config;
import org.protorabbit.model.IProperty;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class InsertCommand extends BaseCommand {

    public static final String DEFERRED_PROPERTIES = "DEFERRED_PROPERTIES";

    @SuppressWarnings("unchecked")
    @Override
    public void doProcess(OutputStream out) throws IOException {

        Config cfg = ctx.getConfig();
        IProperty p = cfg.getContent(ctx.getTemplateId(), params[0], ctx);
        if (p != null && p.getDefer()) {
            Map<String, String> deferredProperties = (Map<String, String>)ctx.getAttribute(DEFERRED_PROPERTIES);
            if (deferredProperties == null) {
                deferredProperties = new HashMap<String,String>();
            }
            deferredProperties.put(p.getKey(),p.getValue());
            ctx.setAttribute(DEFERRED_PROPERTIES, deferredProperties);
            String div = "<div id=\"" + ctx.getTemplateId() + "_" + p.getKey() + "\">" +
                ((p.getDeferContent() != null) ? p.getDeferContent().toString() : "") + 
            "</div>"; 
            out.write(div.getBytes());
        } else if (p != null) {
            out.write(p.getValue().getBytes());
        } else {
            String message = "InsertWarning: Unable find property " + params[0];
            Config.getLogger().warning(message);
        }
    }

}
