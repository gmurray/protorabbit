package org.protorabbit.accelerator.impl;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import org.protorabbit.accelerator.CacheContext;
import org.protorabbit.accelerator.ICacheable;
import org.protorabbit.json.Serialize;
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
    private Long timeout = null;
    private static Logger logger = null;

    private long accessCount = 0;
    private long gzipAccessCount = 0;

    static Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("org.protrabbit");
        }
        return logger;
    }

    public CacheableResource() {}

    public CacheableResource(String contentType,
                            Long maxAge,
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
    public long getContentLength() {
        if (content == null) return 0;
        else return content.length();
    }

    public String getContentType() {
        return contentType;
    }
    
    public void refresh(IContext ctx) {
        // reload
    }


    @Serialize("skip")
    public StringBuffer getContent() {
        Date now = new Date();
        setLastAccessed(now.getTime());
        return content;
    }


    public void setContent(StringBuffer content) {
        setLoaded(false);
        this.content = content;
        // force a recreation of gzipped and contentHash
        contentHash = null;
        gzippedContent = null;
    }

    @Serialize("skip")
    public byte[] getGZippedContent() throws IOException {
        if (gzippedContent == null && content != null) {

            gzippedContent = IOUtil.getGZippedContent(content.toString().getBytes());

        }
        Date now = new Date();
        setLastAccessed(now.getTime());
        return gzippedContent;
    }

    public String getContentHash() {
        if (contentHash == null && content != null) {
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

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public Long getTimeout() {
        return timeout;
    }

    public long getAccessCount() {
        return accessCount;
    }
    
    public long getGzipAccessCount() {
        return gzipAccessCount;
    }

    public long getGzipContentLength() {
        if (gzippedContent == null) {
            return 0;
        }
        return gzippedContent.length;
    }

    public void incrementAccessCount() {
        accessCount +=1;
        
    }

    public void incrementGzipAccessCount() {
        gzipAccessCount +=1;
        
    }
}