package org.protorabbit.stats;

import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.protorabbit.stats.impl.StatsManager.Resolution;

public interface IStatRecorder {
    public void enableRecording( boolean enable );
    public void init( ServletContext ctx);
    public void recordStatItem( IStat stat );
    public void shutdown();
    public void cleanup();
    public boolean canRecordStats();
    public boolean isEnabled();
    public List<Long> getArchiveTimestamps();
    public List<Long> getSummaryTimestamps();
    public Map<String, Object> loadStatsForDate(long timestamp);
    public Object loadSummaryForDate(long timestamp);
    public Object loadSummarySinceDate( long timestamp, Resolution r );
    public void updateDailySummary();
    public Object loadArchivedStatsForRange( long statTimestamp, long endTimestamp, Resolution r );
    public List<IStat> loadArchivedStatsItemsForRange( long statTimestamp, long endTimestamp );
}
