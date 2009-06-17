package org.protorabbit.accelerator.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    protected int status = -1;

    private long lastAccessed = -1;
    private Long timeout = null;
    private static Logger logger = null;

    private long accessCount = 0;
    private long gzipAccessCount = 0;

    private Map<String, ICacheable> freeAgents;
    private List<String> tests;

    static Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("org.protrabbit");
        }
        return logger;
    }

    public CacheableResource() {
    }

    public CacheableResource(String contentType,
                            Long maxAge,
                            String hash) {

        this.setTimeout(maxAge);
        this.contentType =  contentType;
        this.content = new StringBuffer();
        this.contentHash = hash;
        Long tmax = null;
        if (maxAge != null) {
            tmax = new Long(maxAge.longValue());
        }
        cc = new CacheContext(tmax, hash);
        this.setStatus(200);
    }

    public void addUserAgentResource(String ua, ICacheable ic) {
        if (tests == null) {
            tests = new ArrayList<String>();
        }
        if (freeAgents == null) {
            freeAgents = new HashMap<String, ICacheable>();
        }
        tests.add(ua);
        freeAgents.put(ua, ic);
    }

    public boolean gzipResources() {
        return gzip;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setGzipResources(boolean gzip) {
        this.gzip = gzip;
    }

    public void reset() {
        setLoaded(false);
        lastAccessed = -1;
        this.content = new StringBuffer();
        this.gzippedContent = null;
        cc.reset();
    }

    public CacheContext getCacheContext() {
        return cc;
    }

    public void appendContent(String ncontent) {
        setLoaded(false);
        content.append(ncontent);
        // force a recreation of gzipped and contentHash
        contentHash = null;
        gzippedContent = null;
    } 

    public long getContentLength() {
        if (content == null) return 0;
        else return content.length();
    }

    public String getContentType() {
        return contentType;
    }

    public void refresh(IContext ctx) {
        // TODO : rework
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

    public Map<String,ICacheable> getUserAgentResources() {

        return freeAgents;
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
        if (cc == null) {
            cc = new CacheContext(null, null);
        }
        Long tmax = null;
        if (timeout != null) {
            timeout = new Long(timeout.longValue());
        }
        cc.setMaxAge(tmax);
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

    public ICacheable getResourceForUserAgent(String test) {
        if (test == null) {
            return null;
        }
        if (tests == null) {
            return null;
        }
        for (String target : tests) {
            if (target.equals(test)) {
                return freeAgents.get(target);
            }
        }
        return null;
    }

    public boolean hasUATests() {
        if (tests == null) {
            return false;
        } else if (tests.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    public void setHash(String hash) {
        if (cc == null) {
            cc = new CacheContext(null, null);
        }
        this.contentHash = hash;
        cc.setHash(hash);
    }
    
    public String toString() {
    	String c = "N/A";
    	if (content != null && content.length() > 20) {
    		c = content.substring(0, 20);
    	}
 	   return contentType + " { status : " + status + " content: " + c + "}";
    }
}