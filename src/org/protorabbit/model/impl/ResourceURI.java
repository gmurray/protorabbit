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

import org.protorabbit.model.IContext;
import org.protorabbit.model.ITestable;

public class ResourceURI implements ITestable {

    public static final int SCRIPT = 1;
    public static final int LINK = 2;
    public static final int TEMPLATE = 3;

    private String uri = null;
    private String baseURI = null;

    private boolean written = false;
    private Boolean combine = null;
    private boolean gzip = false;
    private int type = -1;
    private String id = null;
    private String mediaType = null;
    private String test = null;
    private int loadIndex;
    private boolean defer = false;
    private long lastUpdated = -1;
    private String uaTest = null;

    public ResourceURI(String uri, String baseURI, int type) {
        this.uri = uri;
        this.baseURI = baseURI;
        this.type = type;
    }
    
    public int getLoadIndex() {
        return loadIndex;
    }

    public void setLoadIndex(int index) {
        this.loadIndex = index;
    }

    public boolean isWritten() {
        return written;
    }
    
    public void setWritten(boolean written) {
        this.written = written;
    }

    public String getURI() {
        return uri;
    }
    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getBaseURI() {
        return baseURI;
    }

    public void setbaseURI(String baseURI) {
        this.baseURI = baseURI;
    }
    public Boolean getCombine() {
        return combine;
    }
    public void setCombine(Boolean combine) {
        this.combine = combine;
    }
    public int getType() {
        return type;
    }
    public void setType(int type) {
        this.type = type;
    }

    public boolean isExternal() {
        if (uri != null) {
            return uri.startsWith("http");
        }
        return false;
    }

    public void setGzip(boolean gzip) {
        this.gzip = gzip;
    }

    public boolean isGzip() {
        return gzip;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        if (id == null) {
            return getFullURI();
        } else {
            return id;
        }
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getFullURI() {
        return baseURI + uri;
    }

    public String getTest() {
        return test ;
    }

    public void setTest(String test) {
        this.test = test;
    }

    public void setDefer(boolean defer) {
        this.defer = defer;
    }

    public boolean isDefer() {
        return defer;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void updateLastUpdated(IContext ctx) {
        this.lastUpdated = ctx.getLastUpdated(getFullURI());
    }

    public boolean isUpdated(IContext ctx) {
        return ctx.isUpdated(getFullURI(), lastUpdated);
    }

    public String getUATest() {
        return this.uaTest;
    }

    public void setUATest(String test) {
        this.uaTest = test;
    }
}
