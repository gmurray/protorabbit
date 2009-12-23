package org.protorabbit.test;

public class ClientDetails {

    private String url;
    private long timeout = 2500;
    private boolean useRandomTimeout = true;
    private int runCount = 1;
    private String id = null;
    private long expectedMaxContentLength = 0;
    private long expectedMinContentLength = -1;

    public ClientDetails() {}

    public String getId() {
        return id;
    }

    public void setId( String id ) {
        this.id = id;
    }

    public void setUrl( String url ) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUseRandomTimeout( boolean random ) {
        this.useRandomTimeout = random;
    }
    
    public boolean getUseRandomTime() {
        return useRandomTimeout;
    }

    public void setTimeout( long timeout ) {
        this.timeout = timeout;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setRunCount( int runCount ) {
        this.runCount = runCount;
    }

    public int getRuncount() {
        return runCount;
    }

    public long getExpectedMinContentLength() {
        return expectedMinContentLength;
    }

    public void setExpectedMinContentLength( long expectedMinContentLength) {
        this.expectedMinContentLength = expectedMinContentLength;
    }
    public long getExpectedMaxContentLength() {
        return expectedMaxContentLength;
    }

    public void setExpectedMaxContentLength( long expectedMaxContentLength) {
        this.expectedMaxContentLength = expectedMaxContentLength;
    }

}
