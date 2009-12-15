package org.protorabbit.stats.impl;

import org.protorabbit.stats.IResourceStat;

public class ResourceStat implements IResourceStat {

    private Long totalProcessingTime;
    private Long totalContentLength;
    private Double averageProcessingTime;
    private Double averageContentLength;
    private Integer accessCount;

    public Integer getAccessCount() {

        return accessCount;
    }

    public Double getAverageContentLength() {
        return averageContentLength;
    }

    public Double getAverageProcessingTime() {
        return averageProcessingTime;
    }

    public Long getTotalContentLength() {
        return totalContentLength;
    }

    public Long getTotalProcessingTime() {
        return totalProcessingTime;
    }

    public void setAccessCount( Integer accessCount) {
        this.accessCount = accessCount;

    }

    public void setAverageContentLength( Double averageContentLength ) {
        this.averageContentLength = averageContentLength;

    }

    public void setAverageProcessingTime( Double averageProcessingTime ) {
        this.averageProcessingTime = averageProcessingTime;
    }

    public void setTotalContentLength( Long totalContentLength ) {
        this.totalContentLength = totalContentLength;
    }

    public void setTotalProcessingTime( Long totalProcessingTime ) {
        this.totalProcessingTime = totalProcessingTime;
    }

}
