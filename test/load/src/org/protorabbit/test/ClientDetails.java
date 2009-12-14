package org.protorabbit.test;

public class ClientDetails {

    private String url;
    private long timeout = 2500;
    private boolean useRandomTimeout = true;
    private int runCount = 1;
    private String id = null;

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
}
