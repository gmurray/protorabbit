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
    private String fullURI = null;

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
    private Boolean uniqueURL = null;
    private String uaTest = null;
    private long created = 0;

    public ResourceURI(String uri, String baseURI, int type) {
        this.uri = uri;
        this.baseURI = baseURI;
        this.type = type;
        this.created = System.currentTimeMillis();
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
        return getURI(null);
    }

    public String getURI(Boolean unique) {
        String turi = uri;
        if ((unique != null && unique == Boolean.TRUE) || (uniqueURL != null && uniqueURL == Boolean.TRUE)) {
            turi += "?puuid=" + created;
        }
        return turi;
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
        if (fullURI == null) {
            fullURI = baseURI;

        if (uri.startsWith("/")) {
                fullURI = uri;
        // make sure to prevent // in paths
        } else if (uri.startsWith("/") && baseURI.endsWith("/")) {
                fullURI += uri.substring(1);

            } else {
                fullURI += uri;
            }
        }
        return fullURI;
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
        if (uri.startsWith("/")) {
            this.lastUpdated = ctx.getLastUpdated(uri);
        } else {
            this.lastUpdated = ctx.getLastUpdated(getFullURI());
        }
    }

    public boolean isUpdated(IContext ctx) {
        return ctx.isUpdated(getFullURI(), lastUpdated);
    }

    public String getUATest() {
        return uaTest;
    }

    public void setUATest(String test) {
        this.uaTest = test;
    }

    public void setUniqueURL(Boolean uniqueURL) {
        this.uniqueURL = uniqueURL;
    }

    public Boolean getUniqueURL() {
        return uniqueURL;
    }

    public long getCreateTime() {
        return created;
    }
}
