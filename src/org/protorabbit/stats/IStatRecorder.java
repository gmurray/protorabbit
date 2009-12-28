package org.protorabbit.stats;

import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;


public interface IStatRecorder {
    public void init( ServletContext ctx);
    public void recordStatItem( IStat stat );
    public void shutdown();
    public void cleanup();
    public boolean canRecordStats();
    public List<Long> getArchiveTimestamps();
    public List<Long> getSummaryTimestamps();
    public Map<String, Object> loadStatsForDate(long timestamp);
    public Object loadSummaryForDate(long timestamp);
}
