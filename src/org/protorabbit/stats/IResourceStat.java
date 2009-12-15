package org.protorabbit.stats;

public interface IResourceStat {
    // getters
    public Integer getAccessCount();
    public Double getAverageProcessingTime();
    public Long getTotalProcessingTime();
    public Double getAverageContentLength();
    public Long getTotalContentLength();
    // setters
    public void setTotalContentLength( Long totalContentLength );
    public void setAverageContentLength( Double averageContentLength );
    public void setAverageProcessingTime( Double averageProcessingTime );
    public void setAccessCount( Integer accessCount);
    public void setTotalProcessingTime( Long totalProcessingTime );
}
