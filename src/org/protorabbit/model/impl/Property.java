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

import org.protorabbit.model.IProperty;


public class Property implements IProperty {

    private String baseURI = null;
    private String value = null;
    private String key = null;
    private int type = -1;
    private String originalTemplate = null;
    private Long timeout = null;
    private String test = null;
    private boolean defer;
    private StringBuffer deferContent = null;
    private String uaTest = null;
    private String id = null;

    public Property(String key, String value, int type, String baseURI, String originalTemplate) {
        this.key = key;
        this.value = value;
        this.type = type;
        this.baseURI = baseURI;
        this.originalTemplate = originalTemplate;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getId() {
        return id;
    }

    public String getBaseURI() {
        return baseURI;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public int getType() {
        return type;
    }

    public String originalTemplateId() {
        return originalTemplate;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public String getTest() {
        return test;
    }

    public void setTest(String t) {
        this.test = t;
    }

    public void setDefer(boolean defer) {
        this.defer = defer;
    }

    public boolean getDefer() {
        return defer;
    }

    public void setDeferContent(StringBuffer deferContent) {
        this.deferContent = deferContent;
    }

    public StringBuffer getDeferContent() {
        return deferContent;
    }

    public String getUATest() {
        return uaTest ;
    }

    public void setUATest(String test) {
        this.uaTest = test;
    }
}
