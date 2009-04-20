package org.protorabbit.accelerator;

import java.io.IOException;

import org.protorabbit.IOUtil;

/**
 *  A class for representing combined cacheable resources.
 *  
 * 
 * @author gmurray
 */
public class CacheableResource {

    protected byte[] gzippedContent;
    protected StringBuffer content;
    protected String contentType;
    protected CacheContext cc = null;
    protected boolean gzip = true;

    protected String hash;
    protected String contentHash;

    public CacheableResource(String contentType,
                            long maxAge,
                            String hash) {

        this.contentType =  contentType;
        this.content = new StringBuffer();
        this.contentHash = hash;
        cc = new CacheContext(maxAge, hash);
    }

    public boolean gzipResources() {
        return gzip;
    }

    public void setGzipResources(boolean gzip) {
        this.gzip = gzip;
    }

    public void reset() {
        this.content = new StringBuffer();
        this.gzippedContent = null;
        cc.reset();
    }

    public CacheContext getCacheContext() {
        return cc;
    }

    public void appendContent(String ncontent) {
        content.append(ncontent);
        // force a recreation of gzipped and contentHash
        contentHash = null;
        gzippedContent = null;
    } 

    public long getContentLegth() {
        if (content == null) return 0;
        else return content.length();
    }

    public String getContentType() {
        return contentType;
    }

    public StringBuffer getContent() {
        return content;
    }

    public void setContent(StringBuffer content) {
        this.content = content;
        // force a recreation of gzipped and contentHash
        contentHash = null;
        gzippedContent = null;
    }

    public byte[] getGZippedContent() throws IOException {
        if (gzippedContent == null && getContent() != null) {
            gzippedContent = IOUtil.getGZippedContent(getContent().toString().getBytes());

        }
        return gzippedContent;
    }

    public String getContentHash() {
        if (contentHash == null) {
            contentHash = IOUtil.generateHash(content.toString());
        }
        return contentHash;
    }
}