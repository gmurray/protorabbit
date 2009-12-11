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
import org.protorabbit.model.IContext;
import org.protorabbit.model.IParameter;
import org.protorabbit.model.IProperty;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class InsertCommand extends BaseCommand {

    public static final String DEFERRED_PROPERTIES = "DEFERRED_PROPERTIES";

    private static Logger logger = null;

    static Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("org.protrabbit");
        }
        return logger;
    }

    @SuppressWarnings("unchecked")
    public void doProcess(IContext ctx) throws IOException {

        Config cfg = ctx.getConfig();

        String id = null;
        if (params.length > 0 && params[0].getType() == IParameter.STRING) {
            id = params[0].getValue().toString();
        } else {
            getLogger().severe("Error processing property " + params[0].getValue().toString() + " Parameter is not of type String");
            return;
        }

        IProperty p = cfg.getProperty(ctx.getTemplateId(), id, ctx);
        if (p == null) {
            return;
        }
        String value = null;
        if (p != null) {
            value = p.getValue();
        } else {
            getLogger().warning("Non fatal error : null property " + id + " processing template " + ctx.getTemplateId());
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
                        replacementString = replacement + "";
                    }
                    evalue += replacementString;

                } else {
                    getLogger().warning("Non fatal error parsing expression for property " + p.getKey() +
                             ". Expression does not contain a closing }");
                    evalue = null;
                    break;
                }
                index = currentEnd + 1;
                current= value.indexOf("${", index);
            }
            if (evalue != null) {
                // tack on the rest of the string
                if (value.length() > index) {
                    evalue += value.substring(index);
                }
                value = evalue;
            }
        }
        if (p != null && p.getDefer() != null && p.getDefer() == true) {
            Map<String, String> deferredProperties = (Map<String, String>)ctx.getAttribute(DEFERRED_PROPERTIES);
            if (deferredProperties == null) {
                deferredProperties = new HashMap<String,String>();
            }
            deferredProperties.put(p.getKey(),value);
            ctx.setAttribute(DEFERRED_PROPERTIES, deferredProperties);
            String span = "<span id=\"" + ctx.getTemplateId() + "_" + p.getKey() + "\">" +
                ((p.getDeferContent() != null) ? p.getDeferContent().toString() : "") + 
            "</span>"; 
            getBuffer(ctx).write(span.getBytes());
        } else if (p != null) {
            getBuffer(ctx).write(value.getBytes());
        } else {
            String message = "InsertWarning: Unable find property " + params[0];
            getLogger().warning(message);
        }
    }

}
