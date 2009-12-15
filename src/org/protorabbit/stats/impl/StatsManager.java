package org.protorabbit.stats.impl;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.protorabbit.stats.IStat;
import org.protorabbit.stats.IResourceStat;

public class StatsManager {

    private List<IStat> stats = null;
    private static StatsManager statManager = null;
    private static Hashtable<String, Map<String, IResourceStat>> pageStats;

    // prevent non use of constructor
    private StatsManager() {
        stats =  new ArrayList<IStat>();
        pageStats = new Hashtable<String, Map<String,IResourceStat>>();
    }

    public static StatsManager getInstance() {
        if (statManager == null) {
            statManager = new StatsManager();
        }
        return statManager;
    }

    @SuppressWarnings("unchecked")
    public List<IStat> getStats() {
        if ( stats == null) {
            return null;
        }
        synchronized (stats) {
            return (List<IStat>) ((ArrayList)stats).clone();
        }
    }

    public Map<String, Map<String,IResourceStat>> getPageStats() {
        return pageStats;
    }

    public void add( IStat s ) {

        synchronized(stats) {
            stats.add( s );
            // add to the page stats
            Map<String, IResourceStat> cstats = pageStats.get( s.getContentType() );
            if ( cstats == null ) {
                cstats = new Hashtable<String,IResourceStat>();
                pageStats.put( s.getContentType(), cstats );
            }
            String key = s.getPath();
            IResourceStat c = cstats.get( key );
            if ( c == null) {
                c = new ResourceStat();
                c.setAccessCount(1);
                c.setAverageContentLength( new Double(s.getContentLength()) );
                c.setTotalContentLength( s.getContentLength());
                c.setAverageProcessingTime( new Double(s.getProcessTime()) );
                c.setTotalProcessingTime( s.getProcessTime() );
                cstats.put( key,c );
            } else {
                int count = c.getAccessCount().intValue();
                c.setAccessCount( ++count );
                long tLength = c.getTotalContentLength().longValue() + s.getContentLength().longValue();
                c.setTotalContentLength( new Long(tLength) );
                double averageLength = ( (double)tLength / (double)count );
                c.setAverageContentLength( new Double(averageLength) );
                Long totalProcessTime = c.getTotalContentLength().longValue() + s.getProcessTime().longValue();
                double averageProcessTime = ( (double)totalProcessTime / (double)count );
                c.setAverageProcessingTime( new Double(averageProcessTime) );
                c.setTotalProcessingTime( totalProcessTime );
                cstats.put( key, c );
            }
        }
    }

    public List<IStat> getLatest( long duration) {
        List<IStat> baseList = getStats();
        if ( baseList == null ) {
            return null;
        }
        List<IStat> cStats = new ArrayList<IStat>();
        long threshold = System.currentTimeMillis() - duration;
        // iterate the list backwards because it is LIFO and we can stop iterating early if we 
        // find a stat outside the threshold
        for ( int i= baseList.size() -1; i >= 0; i--) {
            IStat stat = baseList.get( i );
            if ( stat.getTimestamp() > threshold ) {
                cStats.add( stat );
                // since we go backwards don't iterate if we have a stat already greater
            } else {
                break;
            }
        }
        return cStats;
    }
}
