package org.protorabbit.accelerator;

import java.io.IOException;

import org.protorabbit.model.IContext;

public interface ICacheable {

    public boolean gzipResources();

    public void setGzipResources(boolean gzip);

    public void reset();

    public CacheContext getCacheContext();

    public void appendContent(String ncontent);

    public long getContentLegth();

    public String getContentType();

    public StringBuffer getContent();

    public void setContent(StringBuffer content);

    public byte[] getGZippedContent() throws IOException;

    public String getContentHash();

    public void setLoaded(boolean loaded);

    public boolean isLoaded();
    
    public void refresh(IContext ctx);
    
    public int getStatus();
    
    public void setStatus(int status);

}