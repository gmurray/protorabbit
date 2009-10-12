package org.protorabbit.communicator;

import java.util.Date;

public class JSONPoller {

    private String id;

    private Long pollInterval;
    private long lastAccess;
    private long pollCount = 0;

    public JSONPoller(String id) {
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

    public void incrementCount() {
        this.lastAccess = (new Date()).getTime();
        pollCount += 1;
    }

    public long getCount() {
        return pollCount;
    }

    public String getId() {
        return id;
    }

}
