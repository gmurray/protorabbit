package org.protorabbit.stats;

public interface IClient {

    public long getLastAccess();

    public void setLastAccess(long lastAccess);

    public void incrementViewCount();
    public void incrementJSONCount();
    public void incrementErrorCount();
    public void incrementPollCount();

    public long getTotalRequestCount();

    public String getClientId();
    public int getErrorCount();
    public long getJSONRequestCount();
    public long getViewRequestCount();
    public void setPollInterval(Long defaultPollInterval);
    public Long getPollInterval();
}
