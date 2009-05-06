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

import java.util.HashMap;
import java.util.Map;

import org.protorabbit.model.IContext;

public  abstract class BaseContext implements IContext {

    private Map<String, Object>attributes;
    private String templateId;

    public BaseContext(){
        this.attributes = new HashMap<String, Object>();
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public Map<String, ?> getAttributes() {
        return attributes;
    }

    public synchronized void setAttribute(String key, Object value) {
        attributes.put(key, value);

    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;

    }

    public String getTemplateId() {
        return templateId;
    }

    public Object parseExpression(String expression) {
        return getAttribute(expression);
    }
}
