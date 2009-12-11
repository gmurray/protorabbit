package org.protorabbit.stats;

public interface IStat {
    public long getTimestamp();
    public void setTimestamp( long timestamp );
    public Enum<?> getType();
    public void setType( Enum<?> type );
    public String getRemoteClient();
    public void setRemoteClient( String remoteClient );
    public String getHost();
    public void setHost( String host );
    public String getRequestURI();
    public void setRequestURI ( String uri );
    public String getPathInfo();
    public void setPathInfo( String pathInfo );
    public String getPath();
    public void setPath( String path );
    public Long getBytesServed();
    public void setBytesServed( Long bytesServed );
    public Long getProcessTime();
    public void setProcessTime( Long processTime );
    public void setContentType( String contentType );
    public String getContentType();
}
