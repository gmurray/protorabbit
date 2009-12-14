package org.protorabbit.stats;

public class StatsItem implements IStat {

    private long timestamp = 0;
    private Enum<?> type = null;
    private String remoteClient;
    private Long processTime = null;
    private String path = null;
    private String pathInfo = null;
    private String host = null;
    private String fullURL = null;
    private Long contentLength = null;
    private String contentType = null;

    public static enum types {
        VIEW,
        JSON
    };

    public Long getContentLength() {
        return contentLength;
    }

    public String getRequestURI() {
        return fullURL;
    }

    public String getHost() {
        return host;
    }

    public String getPath() {
        return path;
    }

    public String getPathInfo() {
        return pathInfo;
    }

    public Long getProcessTime() {
        return processTime;
    }

    public String getRemoteClient() {
        return remoteClient;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Enum<?> getType() {
        return type;
    }

    public void setContentLength( Long contentLength ) {
        this.contentLength = contentLength;
    }

    public void setRequestURI( String fullURL) {
        this.fullURL = fullURL;
    }

    public void setHost( String host ) {
        this.host = host;
    }

    public void setPath( String path ) {
        this.path = path;
    }

    public void setPathInfo( String pathInfo ) {
        this.pathInfo = pathInfo;
    }

    public void setProcessTime( Long processTime ) {
        this.processTime = processTime;
    }

    public void setRemoteClient( String remoteClient ) {
        this.remoteClient = remoteClient;
    }

    public void setTimestamp( long timestamp ) {
        this.timestamp = timestamp;
    }

    public void setType( Enum<?> type ) {
        this.type = type;
    }

    public String getContentType() {
        return contentType ;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

}
