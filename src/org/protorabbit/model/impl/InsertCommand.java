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
        IProperty p = cfg.getProperty(ctx.getTemplateId(), params[0], ctx);
        boolean valid = true;
        if (p.getTest() != null) {
            valid = ctx.test(p.getTest());
        }
        if (p.getUATest() != null) {
            valid = ctx.test(p.getUATest());
        }
        if (!valid) {
            return;
        }
        String value = null;
        if (p != null) {
            value = p.getValue();
        } else {
            Config.getLogger().warning("Non fatal error : null property " + params[0] + " processing template " + ctx.getTemplateId());
            return;
        }
        int current = -1;
        if (value != null) {
            current = value.indexOf("${");
        }
        /*
         *  Write out any expressions - need to optimize this block
         */
        if (current != -1) {
            String evalue = "";
            int index = 0;
            while (current != -1) {
                evalue += value.substring(index,current);
                int currentEnd = value.indexOf("}", current + 2);
                if (currentEnd != -1) {
                    String expression = value.substring(current + 2,currentEnd);
                    Object replacement =  ctx.parseExpression(expression);
                    String replacementString = "";
                    if (replacement != null) {
                        replacementString = replacement.toString();
                    }
                    evalue += replacementString;

                } else {
                    Config.getLogger().warning("Non fatal error parsing expression for property " + p.getKey() +
                             ". Expression does not contain a closing }");
                    evalue = null;
                    break;
                }
                index = currentEnd + 1;
                current= value.indexOf("${", index);
            }
            if (evalue != null) {
                value = evalue;
            }
        }
        if (p != null && p.getDefer()) {
            Map<String, String> deferredProperties = (Map<String, String>)ctx.getAttribute(DEFERRED_PROPERTIES);
            if (deferredProperties == null) {
                deferredProperties = new HashMap<String,String>();
            }
            deferredProperties.put(p.getKey(),value);
            ctx.setAttribute(DEFERRED_PROPERTIES, deferredProperties);
            String span = "<span id=\"" + ctx.getTemplateId() + "_" + p.getKey() + "\">" +
                ((p.getDeferContent() != null) ? p.getDeferContent().toString() : "") + 
            "</span>"; 
            out.write(span.getBytes());
        } else if (p != null) {
            out.write(value.getBytes());
        } else {
            String message = "InsertWarning: Unable find property " + params[0];
            Config.getLogger().warning(message);
        }
    }

}
