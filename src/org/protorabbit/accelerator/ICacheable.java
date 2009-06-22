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

package org.protorabbit.accelerator;

import java.io.IOException;

import org.protorabbit.model.IContext;

public interface ICacheable {

    public static final int LOADING = 100;
    public static final int LOADED = 200;
    public static final int INITIALIZED = 99;

    public boolean gzipResources();

    public Long getTimeout();

    public void setTimeout(Long timeout);

    public void setGzipResources(boolean gzip);

    public void reset();

    public long getAccessCount();

    public void incrementAccessCount();

    public long getGzipAccessCount();

    public void incrementGzipAccessCount();

    public CacheContext getCacheContext();

    public void appendContent(String ncontent);

    public long getContentLength();

    public long getGzipContentLength();

    public String getContentType();

    public int getResourceType();

    public void setContentType(String contentType);

    public StringBuffer getContent();

    public void setContent(StringBuffer content);

    public byte[] getGZippedContent() throws IOException;

    public String getContentHash();

    public void setLoaded(boolean loaded);

    public boolean isLoaded();

    public void refresh(IContext ctx);

    public int getStatus();

    public void setStatus(int status);

    public void setLastAccessed(long lastAccessed);

    public long getLastAccessed();

    public boolean hasUATests();

    public ICacheable getResourceForUserAgent(String test);

    public void setHash(String hash);

    public void addUserAgentResource(String userAgent, ICacheable csr);

}