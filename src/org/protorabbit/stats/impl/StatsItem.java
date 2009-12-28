package org.protorabbit.stats.impl;

import org.protorabbit.stats.IStat;

import java.util.Collection;

public class StatsItem implements IStat {

    private long timestamp = 0;
    private IStat.types type = null;
    private String remoteClient;
    private Long processTime = null;
    private String path = null;
    private String pathInfo = null;
    private String host = null;
    private String fullURL = null;
    private Long contentLength = null;
    private String contentType = null;
    private boolean hasErrors = false;
    private Collection<String> errors = null;
    private boolean isPollStat = false;

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

    public void setType( IStat.types type ) {
        this.type = type;
    }

    public String getContentType() {
        return contentType ;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public boolean hasErrors() {
        return hasErrors;
    }

    public void setHasErrors(boolean hasErrors) {
        this.hasErrors = hasErrors;
    }

    public Collection<String> getErrors() {
        return errors;
    }

    public void setErrors(Collection<String> errors) {
        this.errors = errors;
    }

    public void setIsPoller(boolean pollerstat) {
        this.isPollStat = pollerstat;
    }

    public boolean isPoller() {
        return isPollStat;
    }

    public String toString() {
        return "StatsItem { path : " + path + ", contentType" + contentType + ", timestamp : " + timestamp + ", type : " + type + " }";
    }
}
