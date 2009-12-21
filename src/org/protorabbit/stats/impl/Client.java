package org.protorabbit.stats.impl;

import java.util.Date;
import org.protorabbit.stats.IClient;

public class Client implements IClient {

    private String id;
    private long viewRequestCount = 0;
    private long jsonRequestCount = 0;
    private long pollRequestCount = 0;
    private long lastAccess;
    private Long pollInterval = 5000L;
    private int errorCount = 0;

    public Client(String id) {
            this.id = id;
            this.lastAccess = (new Date()).getTime();
    }

    public Long getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(Long pollInterval) {
        this.pollInterval = pollInterval;
    }

    public long getLastAccess() {
        return lastAccess;
    }

    public void setLastAccess(long lastAccess) {
        this.lastAccess = lastAccess;
    }

    public void incrementJSONCount() {
        this.lastAccess = (new Date()).getTime();
        jsonRequestCount += 1;
    }

    public void incrementViewCount() {
        this.lastAccess = (new Date()).getTime();
        viewRequestCount += 1;
    }

    public void incrementErrorCount() {
        errorCount += 1;
    }

    public long getTotalRequestCount() {
        return  (jsonRequestCount + viewRequestCount);
    }

    public long getJSONRequestCount() {
        return jsonRequestCount;
    }

    public long getViewRequestCount() {
        return viewRequestCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public String getClientId() {
        return id;
    }

    public void incrementPollCount() {
        this.lastAccess = (new Date()).getTime();
        pollRequestCount += 1;
    }

}
