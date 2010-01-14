package org.protorabbit.stats.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.protorabbit.stats.IClient;
import org.protorabbit.stats.IClientIdGenerator;
import org.protorabbit.stats.IResourceStat;
import org.protorabbit.stats.IStat;
import org.protorabbit.stats.IStatRecorder;

public class StatsManager implements ServletContextListener {

    public static final String STATS_MANAGER = "org.protorabbit.STATS_MANAGER";
    public static final String CLIENT_ID_GENERATOR = "prt-client-id-generator-class";

    public static final Object APPLICATION_JSON = "application/json";
    public static final Object TEXT_HTML = "text/html";
    public static final String JSON_POLLER = "pollers";

    private List<IStat> stats = null;

    private static TimeMonitor tm = null;

    private PollManager pollManager = null;
    private IStatRecorder sr = null;
    private ServletContext ctx = null;

    public enum Resolution {
        SECOND ( 1000 ),
        MINUTE ( 60000 ),
        FIVE_MINUTES ( 300000 ),
        HOUR  ( 3600000 ),
        DAY  ( 86400000 );

        private final long modValue;
        private Resolution( long time ) {
            modValue = time;
        }

        public long modValue() {
            return modValue;
        }
    }

    // prevent non use of constructor
    public StatsManager() {
        PollManager.getInstance();
        stats =  new ArrayList<IStat>();
        tm = new TimeMonitor( this );
        tm.start();
    }

    private static Logger logger = null;

    static final Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("org.protrabbit");
        }
        return logger;
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

    public Map<String, IClient> getPollers() {
        return pollManager.getPollers();
    }

    public void add( IStat s ) {

        synchronized(stats) {
            stats.add( s );
            sr.recordStatItem( s );
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

    public static Long getRoundTime( long resolution, long timestamp) {
        long mod = timestamp % resolution;
        return new Long( (timestamp - mod) );
    }

    public static class TimeChartItem {

        private long timestamp = 0;
        private double value = 0;
        private long contentLength = 0;
        private long processingTime = 0;

        public TimeChartItem( long timestamp ) {
           this.timestamp = timestamp;
        }

        public TimeChartItem( long timestamp, double value ) {
            this.timestamp = timestamp;
            this.value = value;
         }

        public void addContentLength( double _value ) {
            this.contentLength += _value;
        }

        public long totalProcessingTime() {
            return processingTime;
        }

        public double averageProcessingTime() {
            if ( processingTime > 0 ) {
                return ( (double)processingTime / value );
            } else {
                return 0;
            }
        }

        public void addProcessingTime( long pt ) {
            processingTime += pt;
        }

        public double averageContentLength() {
            if ( contentLength > 0 ) {
                return ( (double)contentLength / value );
            } else {
                return 0;
            }
        }

        // don't use a getter to prevent serialization
        public long contentLength() {
            return contentLength;
        }
 
        public void incrementValue() {
            value += 1;
        }

        public long getTime() {
            return timestamp;
        }
        public void setY( double nValue) {
            this.value = nValue;
        }

        public double getY() {
            return value;
        }
    }

    public Map<String, Object> getLatest( long duration, Resolution r ) {
        List<IStat> baseList = getStats();
        return getStats( duration, r, baseList );
    }

    public static Map<String, Object> getStats( Long duration, Resolution r, List<IStat> baseList ) {

        if ( baseList == null ) {
            return null;
        }
        long resolution = r.modValue();
        Map<Long,TimeChartItem> vBuckets = new LinkedHashMap<Long,TimeChartItem>();
        Map<Long,TimeChartItem> jBuckets = new LinkedHashMap<Long,TimeChartItem>();

        Map<String, Map<String,IResourceStat>> pageStats = new Hashtable<String, Map<String,IResourceStat>>();
        Map<String,IClient> clients = new HashMap<String, IClient>();
        List<IStat> errors = new ArrayList<IStat>();
        long threshold = -1;
        if (duration != null) {
            threshold = System.currentTimeMillis() - duration;
        }
        long totalCount = 0;
        // do the processing
        totalCount = addStats( duration, resolution, baseList,vBuckets, jBuckets,
                 clients, pageStats,  errors,
                 threshold  ) ;

        return createStatsEnvelope( vBuckets, jBuckets,
                clients, pageStats,  errors,
                threshold,  totalCount ) ;
      }

    public static Map<String, Object> createStatsEnvelope(   Map<Long,TimeChartItem> vBuckets, Map<Long,TimeChartItem> jBuckets,
                                                Map<String,IClient> lclients, Map<String, Map<String,IResourceStat>> pageStats, List<IStat> errors,
                                                long threshold, long long1  ) {
        Map<String, Object>jds = new HashMap<String,Object>();
        jds.put("label", "application/json");
        jds.put("yaxis", new Long(1) );
        // sort the buckets
        List<TimeChartItem> sorted = new ArrayList<TimeChartItem>();
        sorted.addAll( jBuckets.values() );
        Collections.sort( sorted, new ResourceStatComparator() );
        jds.put("values", sorted );

        // views
        Map<String, Object>vds = new HashMap<String,Object>();
        vds.put("label", "text/html");
        vds.put("yaxis", new Long(1) );
        //sort
        List<TimeChartItem> vsorted = new ArrayList<TimeChartItem>();
        vsorted.addAll( vBuckets.values() );
        Collections.sort( vsorted, new ResourceStatComparator() );
        vds.put("values", vsorted );

        Map<String, Object> envelope = new HashMap<String, Object>();
        envelope.put("json", jds );
        envelope.put("view", vds );
        envelope.put("errors", errors );
        envelope.put("clients", lclients );
        envelope.put("pageStats",  pageStats );
        envelope.put("total", new Long( long1 ) );
        // create average times
        addPayloadsAndProcessingTime( jBuckets, envelope, "application/json", "JSON" );
        addPayloadsAndProcessingTime( vBuckets, envelope, "text/html", "View" );

        return envelope;
    }

    static class ResourceStatComparator implements Comparator<TimeChartItem> {

        public int compare(TimeChartItem o1, TimeChartItem o2) {
            if (  o1.getTime() > o2.getTime() ) {
                return 1;
            } else if (  o1.getTime() < o2.getTime() ) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    public Object loadArchivedStatsForRange( long statTimestamp, long endTimestamp, Resolution r ) {
        if ( sr != null) {
            return sr.loadArchivedStatsForRange( statTimestamp, endTimestamp, r );
        } else {
            return null;
        }
    }

    public static long addStats( Long duration, long resolution, List<IStat> baseList,
            Map<Long,TimeChartItem> vBuckets, Map<Long,TimeChartItem> jBuckets,
                                                Map<String,IClient> lclients, Map<String, Map<String,IResourceStat>> pageStats, List<IStat> errors,
                                                long threshold ) {
        long counter = 0;
        // iterate the list backwards because it is LIFO and we can stop iterating early if we 
        // find a stat outside the threshold
        for ( int i= baseList.size() -1; i >= 0; i--) {
            IStat stat = baseList.get( i );

            if ( (threshold == -1) || ((threshold != -1) && stat.getTimestamp() > threshold) ) {
                counter++;
                String key = stat.getPath();
                String cid = stat.getRemoteClient();
                Long bucketId = getRoundTime( resolution, stat.getTimestamp() );
                String contentKey = null;
                if ( stat.isPoller() ) {
                    contentKey = JSON_POLLER;
                } else {
                    contentKey = stat.getContentType();
                }
                 Map<String, IResourceStat> cstats = pageStats.get( contentKey );
                if ( cstats == null ) {
                    cstats = new Hashtable<String,IResourceStat>();
                    pageStats.put( contentKey, cstats );
                }
                IResourceStat c = cstats.get( key );
                if ( c == null) {
                    c = new ResourceStat();
                    c.setAccessCount(1);
                    c.setAverageContentLength( new Double(stat.getContentLength()) );
                    c.setTotalContentLength(stat.getContentLength());
                    c.setAverageProcessingTime( new Double(stat.getProcessTime()) );
                    c.setTotalProcessingTime(stat.getProcessTime() );
                    cstats.put( key,c );
                } else {
                    int count = c.getAccessCount().intValue();
                    c.setAccessCount( ++count );
                    long tLength = c.getTotalContentLength().longValue() + stat.getContentLength().longValue();
                    c.setTotalContentLength( new Long(tLength) );
                    double averageLength = ( (double)tLength / (double)count );
                    c.setAverageContentLength( new Double(averageLength) );
                    Long totalProcessTime = c.getTotalContentLength().longValue() + stat.getProcessTime().longValue();
                    double averageProcessTime = ( (double)totalProcessTime / (double)count );
                    c.setAverageProcessingTime( new Double(averageProcessTime) );
                    c.setTotalProcessingTime( totalProcessTime );
                    cstats.put( key, c );
                }
                IClient client = lclients.get( cid );
                if ( client == null ) {
                    client = new Client( cid );
                    lclients.put( cid, client );
                }
                if ( stat.getTimestamp() > client.getLastAccess() ) {
                    client.setLastAccess( stat.getTimestamp() );
                }
                if ( stat.getContentType().equals( APPLICATION_JSON ) ) {
                    client.incrementJSONCount();
                } else if (stat.getContentType().equals( TEXT_HTML )){
                    client.incrementViewCount();
                }
                if ( stat.hasErrors() ){
                    errors.add( stat );
                    client.incrementErrorCount();
                } 

                if (TEXT_HTML.equals(stat.getContentType()) ) {
                    TimeChartItem tci = vBuckets.get( bucketId );
                    if ( tci == null) {
                        tci = new TimeChartItem( bucketId );
                        vBuckets.put( bucketId, tci  );
                    }
                    tci.incrementValue();
                    tci.addContentLength( stat.getContentLength() );
                    tci.addProcessingTime( stat.getProcessTime() );
                } else if (APPLICATION_JSON.equals(stat.getContentType()) ) {
                    TimeChartItem tci = jBuckets.get( bucketId );
                    if ( tci == null) {
                        tci = new TimeChartItem( bucketId );
                        jBuckets.put( bucketId, tci  );
                    }
                    tci.incrementValue();
                    tci.addContentLength( stat.getContentLength() );
                    tci.addProcessingTime( stat.getProcessTime() );

                }
                // since we go backwards don't iterate if we have a stat already greater
            } else {
                if ( threshold != -1 ) {
                    break;
                }
            }
        }
        return counter;
    }

    public  Map<String, Object> getStatsForDate( long timestamp) {
        if ( sr != null ) {
            return sr.loadStatsForDate( timestamp );
        }
        return null;
    }

    public  Object getSummaryForDate( long timestamp) {
        if ( sr != null ) {
            return sr.loadSummaryForDate( timestamp );
        }
        return null;
    }

    public List<Long> getArchiveTimestamps() {
        if ( sr != null ) {
            return sr.getArchiveTimestamps( );
        }
        return null;
    }

    public List<Long> getSummaryTimestamps() {
        if ( sr != null ) {
            return sr.getSummaryTimestamps();
        }
        return null;
    }

    public Object loadSummarySinceDate( long timestamp, Resolution r) {
        if ( sr != null ) {
            return sr.loadSummarySinceDate( timestamp, r );
        }
        return null;
    }

    public List<IStat> loadArchivedStatsItemsForRange( long start, long end ) {
        if ( sr != null ) {
            return sr.loadArchivedStatsItemsForRange( start, end );
        }
        return null;
    }

    private static  void addPayloadsAndProcessingTime( Map<Long,TimeChartItem> jBuckets, Map<String, Object> envelope, String label, String type ) {
        List<TimeChartItem> averageJsonProcessingTimes = new ArrayList<TimeChartItem>();
        List<TimeChartItem> averageJsonPayloads = new ArrayList<TimeChartItem>();
        Iterator<Long> it = jBuckets.keySet().iterator();
        while (it.hasNext()) {
            Long key = it.next();
            TimeChartItem t = jBuckets.get( key );
            averageJsonProcessingTimes.add( new TimeChartItem( key, t.averageProcessingTime() ) );
            averageJsonPayloads.add( new TimeChartItem( key, t.averageContentLength() ) );
        }
        
        Map<String, Object>averageJsonProcessingTimeDS = new HashMap<String,Object>();
        averageJsonProcessingTimeDS.put("label", label);
        averageJsonProcessingTimeDS.put("yaxis", new Long(1) );
        averageJsonProcessingTimeDS.put("values",  averageJsonProcessingTimes);
        envelope.put("average" + type + "ProcessingTime", averageJsonProcessingTimeDS );
        // 
        Map<String, Object>averageJsonPayloadDS = new HashMap<String,Object>();
        averageJsonPayloadDS.put("label", label );
        averageJsonPayloadDS.put("yaxis", new Long(1) );
        averageJsonPayloadDS.put("values",  averageJsonPayloads);

        envelope.put("average" + type + "Payload", averageJsonPayloadDS );
    }

    public IClientIdGenerator getClientIdGenerator( ServletContext ctx) {
        IClientIdGenerator cg = null;
        if (ctx.getInitParameter(CLIENT_ID_GENERATOR) != null) {
            String klassName = (ctx.getInitParameter( CLIENT_ID_GENERATOR )).trim();
            try {
                Class<?> klass = this.getClass().getClassLoader().loadClass( klassName );
                Object target = klass.newInstance();
                cg = (IClientIdGenerator)target;
            } catch (ClassNotFoundException e) {
                getLogger().log( Level.SEVERE, e.getLocalizedMessage() );
                getLogger().log(Level.WARNING, "Will use default client id generator." );
            } catch (InstantiationException e) {
                getLogger().log( Level.SEVERE, e.getLocalizedMessage() );
                getLogger().log(Level.WARNING, "Will use default client id generator." );
            } catch (IllegalAccessException e) {
                getLogger().log( Level.SEVERE, e.getLocalizedMessage() );
                getLogger().log(Level.WARNING, "Will use default client id generator." );
            }
        }
        if (cg == null) {
            cg = new ClientIdGenerator();
        }
        return cg;
    }

    public void contextDestroyed(ServletContextEvent arg0) {
        getLogger().log(Level.INFO, "Shutting down stats monitor....");
        tm.shutdown();
        if (sr != null) {
            sr.cleanup();
        }
    }

    public void pruneHistory( long threshold ) {
        // cleanup 
        if (sr != null) {
            // update the daily summary
            sr.updateDailySummary();
            sr.cleanup();
        }
        int pruneCount = 0;
        synchronized ( stats ) {
            for ( int i= stats.size() -1; i >= 0; i-- ) {
                IStat stat = stats.get( i );
                if ( stat.getTimestamp() < threshold ) {
                    stats.remove( i );
                    pruneCount++;
                }
            }
        }
        if ( pruneCount > 0) {
            getLogger().log( Level.INFO, "StatsManager pruned " + pruneCount + " items." );
        }
    }

    public void contextInitialized( ServletContextEvent e ) {
        ctx  = e.getServletContext();
        ctx.setAttribute( STATS_MANAGER, this );
        sr = new DefaultStatRecorder( );
        sr.init( ctx );
    }

    public void enableStatsRecording( boolean enable ) {
        sr.enableRecording( enable );
    }

    public boolean isStatsRecordingEnabled() {
        return sr.isEnabled();
    }

    public boolean canRecordStats() {
        return sr.canRecordStats();
    }
}
