package org.spv.accelerator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class CacheContext {
	
    private String expires = null;
    // time in milliseconds 
    private long created=0;
    // maximum time in seconds
    private long maxAge=0;
    private SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss  z");
	private String hash;
    
    public CacheContext(long maxAge, String hash) {
    	this.maxAge = maxAge;
    	this.hash = hash;
    	reset();
    }
    
    /*
     * Set the created time to be current and 
     */
    public void reset() {
        created = System.currentTimeMillis();      
        Date expiredate = new Date(created + (maxAge * 1000) );  
        sdf.setTimeZone (TimeZone.getTimeZone("GMT"));
        expires = sdf.format(expiredate);        
    }
    
    public boolean isExpired() {
        long current = System.currentTimeMillis();
        long diff = current - created;
        // convert diff to milliseconds
        return (diff / 1000) > maxAge;
    }

    public long getMaxAge() {
        long current = System.currentTimeMillis();
        long diff = current  + (maxAge * 1000) - created;
        // convert diff to seconds
        return (diff / 1000);
    }
    
    public String getExpires() {
        return expires;
    }

	public long getCreated() {
		return created;
	}

	public void setMaxAge(long maxAge) {
		this.maxAge = maxAge;
	}

	public String getHash() {
		return hash;
	}
}
