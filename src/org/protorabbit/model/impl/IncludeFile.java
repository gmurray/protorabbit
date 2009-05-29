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

import java.util.Date;

import org.protorabbit.model.IContext;

public class IncludeFile {

    private String uri;
    private StringBuffer content;
    private Long timeout;
    private long lastRefresh;
    private int loadIndex;
    private boolean defer = false;
    private StringBuffer deferContent = null;
    private long created;

    public IncludeFile(String uri,
                       StringBuffer content) {

        this.content = content;
        this.uri = uri;
        lastRefresh = created = (new Date()).getTime();
    }

    public String getURI() {
        return uri;
    }

    public long getCreated() {
        return created;
    }

    public StringBuffer getContent() {
        return content;
    }

    public long getContentLength() {
        if (content != null) {
            return content.toString().getBytes().length;
        } else {
            return 0;
        }
    }

    public void setContent(StringBuffer content) {
        this.content = content;
        // reset fresh counter
        lastRefresh = (new Date()).getTime();
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public long getLastRefresh() {
        return lastRefresh;
    }

    public boolean isStale(IContext ctx) {
        if (ctx.getConfig().getDevMode()) {
            boolean isUpdated = ctx.isUpdated( uri, lastRefresh);
            if (isUpdated) return true;
        }
        long now = (new Date()).getTime();
        return (now - lastRefresh > timeout);
    }

    public void setLastRefresh(long lastRefresh) {
        this.lastRefresh = lastRefresh;
    }

    public void setLoadIndex(int loadIndex) {
        this.loadIndex = loadIndex;
    }

    public int getLoadIndex() {
        return loadIndex;
    }

    public void setDefer(boolean defer) {
        this.defer = defer;
    }

    public boolean isDefer() {
        return defer;
    }

    public void setDeferContent(StringBuffer deferContent) {
        this.deferContent = deferContent;
    }

    public StringBuffer getDeferContent() {
        return deferContent;
    }

}
