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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class CacheContext {

    private String expires = null;
    // time in milliseconds 
    private long created = 0;
    // maximum time in seconds
    private Long maxAge = null;
    private SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss  z");
    private String hash;

    public CacheContext(Long maxAge, String hash) {
        this.maxAge = maxAge;
        this.hash = hash;
        reset();
    }

    /*
     * Set the created time to be current and 
     */
    public void reset() {
        created = System.currentTimeMillis();
        if (maxAge == null) {
            return;
        }
        Date expiredate = new Date(created + (maxAge * 1000) );
        sdf.setTimeZone (TimeZone.getTimeZone("GMT"));
        expires = sdf.format(expiredate);
    }

    public boolean isExpired() {
        if (maxAge == null) {
            return true;
        }
        long current = System.currentTimeMillis();
        long diff = current - created;
        // convert diff to milliseconds
        return (diff / 1000) > maxAge;
    }

    public long getMaxAge() {
        if (maxAge == null) {
            return 0;
        }
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

    public void setMaxAge(Long maxAge) {
        this.maxAge = maxAge;
        reset();
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
        
    }
}
