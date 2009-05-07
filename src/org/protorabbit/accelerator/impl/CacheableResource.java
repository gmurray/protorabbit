package org.protorabbit.accelerator.impl;

import java.io.IOException;
import java.util.Date;

import org.protorabbit.Config;
import org.protorabbit.accelerator.CacheContext;
import org.protorabbit.accelerator.ICacheable;
import org.protorabbit.model.IContext;
import org.protorabbit.util.IOUtil;

/**
 *  A class for representing combined cacheable resources.
 *  
 * 
 * @author gmurray
 */
public class CacheableResource implements ICacheable {

    private byte[] gzippedContent;
    private StringBuffer content;
    private String contentType;
    protected CacheContext cc = null;
    private boolean gzip = true;
    private boolean loaded = false;

    protected String hash;
    protected String contentHash;
    private int status = -1;

    private long lastAccessed = -1;
    private long timeout = -1;

    public CacheableResource() {}

    public CacheableResource(String contentType,
                            long maxAge,
                            String hash) {

        this.setTimeout(maxAge);
        this.contentType =  contentType;
        this.content = new StringBuffer();
        this.contentHash = hash;
        cc = new CacheContext(maxAge, hash);
        this.setStatus(200);
    }

    /* (non-Javadoc)
     * @see org.protorabbit.accelerator.ICacheable#gzipResources()
     */
    public boolean gzipResources() {
        return gzip;
    }

    /* (non-Javadoc)
     * @see org.protorabbit.accelerator.ICacheable#setGzipResources(boolean)
     */
    public void setGzipResources(boolean gzip) {
        this.gzip = gzip;
    }

    /* (non-Javadoc)
     * @see org.protorabbit.accelerator.ICacheable#reset()
     */
    public void reset() {
        setLoaded(false);
        status = -1;
        lastAccessed = -1;
        this.content = new StringBuffer();
        this.gzippedContent = null;
        Config.getLogger().info("Resetting " + hash);
        cc.reset();
    }

    /* (non-Javadoc)
     * @see org.protorabbit.accelerator.ICacheable#getCacheContext()
     */
    public CacheContext getCacheContext() {
        return cc;
    }

    /* (non-Javadoc)
     * @see org.protorabbit.accelerator.ICacheable#appendContent(java.lang.String)
     */
    public void appendContent(String ncontent) {
        setLoaded(false);
        content.append(ncontent);
        // force a recreation of gzipped and contentHash
        contentHash = null;
        gzippedContent = null;
    } 

    /* (non-Javadoc)
     * @see org.protorabbit.accelerator.ICacheable#getContentLegth()
     */
    public long getContentLegth() {
        if (content == null) return 0;
        else return content.length();
    }

    /* (non-Javadoc)
     * @see org.protorabbit.accelerator.ICacheable#getContentType()
     */
    public String getContentType() {
        return contentType;
    }
    
    public void refresh(IContext ctx) {
        // reload
    }

    /* (non-Javadoc)
     * @see org.protorabbit.accelerator.ICacheable#getContent()
     */
    public StringBuffer getContent() {
        Date now = new Date();
        setLastAccessed(now.getTime());
        return content;
    }

    /* (non-Javadoc)
     * @see org.protorabbit.accelerator.ICacheable#setContent(java.lang.StringBuffer)
     */
    public void setContent(StringBuffer content) {
        setLoaded(false);
        this.content = content;
        // force a recreation of gzipped and contentHash
        contentHash = null;
        gzippedContent = null;
    }

    /* (non-Javadoc)
     * @see org.protorabbit.accelerator.ICacheable#getGZippedContent()
     */
    public byte[] getGZippedContent() throws IOException {
        if (gzippedContent == null && getContent() != null) {

            gzippedContent = IOUtil.getGZippedContent(getContent().toString().getBytes());

        }
        Date now = new Date();
        setLastAccessed(now.getTime());
        return gzippedContent;
    }

    /* (non-Javadoc)
     * @see org.protorabbit.accelerator.ICacheable#getContentHash()
     */
    public String getContentHash() {
        if (contentHash == null) {
            contentHash = IOUtil.generateHash(content.toString());
        }
        return contentHash;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public void setLastAccessed(long lastAccessed) {
        this.lastAccessed = lastAccessed;
    }

    public long getLastAccessed() {
        return lastAccessed;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public long getTimeout() {
        return timeout;
    }
}