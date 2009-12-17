package org.protorabbit.stats.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.protorabbit.stats.impl.Client;
import org.protorabbit.stats.IClient;
import org.protorabbit.stats.IClientIdGenerator;
import org.protorabbit.stats.IStat;
import org.protorabbit.stats.IResourceStat;

public class StatsManager {

    private static final String CLIENT_ID_GENERATOR = "prt-client-id-generator-class";

    public static final Object APPLICATION_JSON = "application/json";
    public static final Object TEXT_HTML = "text/html";

    private List<IStat> stats = null;

    private static StatsManager statManager = null;

    private PollManager pollManager = PollManager.getInstance();

    // prevent non use of constructor
    private StatsManager() {
        stats =  new ArrayList<IStat>();

    }

    public static StatsManager getInstance() {
        if (statManager == null) {
            statManager = new StatsManager();
        }
        return statManager;
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

    public enum Resolution {
        SECOND ( 1000),
        MINUTE ( 60000),
        HOUR  (360000);

        private final long modValue;
        private Resolution( long time ) {
            modValue = time;
        }

        public long modValue() {
            return modValue;
        }
    }
    
    public Long getRoundTime( long resolution, long timestamp) {
        long mod = timestamp % resolution;
        return new Long( (timestamp - mod) );
    }

    public class TimeChartItem {

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
                System.out.println("getting average " + processingTime + " / " + value + " = " + ( (double)processingTime / value ));
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
        public double getY() {
            return value;
        }
    }

    public Map<String, Object> getLatest( long duration, Resolution r ) {

        List<IStat> baseList = getStats();
        if ( baseList == null ) {
            return null;
        }
        long resolution = r.modValue();
        Map<Long,TimeChartItem> vBuckets = new LinkedHashMap<Long,TimeChartItem>();
        Map<Long,TimeChartItem> jBuckets = new LinkedHashMap<Long,TimeChartItem>();

        Map<String,IClient> lclients = new HashMap<String,IClient>();
        Map<String, Map<String,IResourceStat>> pageStats = new Hashtable<String, Map<String,IResourceStat>>();

        List<IStat> errors = new ArrayList<IStat>();
        long threshold = System.currentTimeMillis() - duration;
        int totalCount = 0;
        // iterate the list backwards because it is LIFO and we can stop iterating early if we 
        // find a stat outside the threshold
        for ( int i= baseList.size() -1; i >= 0; i--) {
            IStat stat = baseList.get( i );

            if ( stat.getTimestamp() > threshold ) {
                totalCount +=1;
                String key = stat.getPath();
                String cid = stat.getRemoteClient();
                Long bucketId = getRoundTime( resolution, stat.getTimestamp() );
                Map<String, IResourceStat> cstats = pageStats.get( stat.getContentType() );
                if ( cstats == null ) {
                    cstats = new Hashtable<String,IResourceStat>();
                    pageStats.put( stat.getContentType(), cstats );
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
                break;
            }
        }
        //
        Map<String, Object>jds = new HashMap<String,Object>();
        jds.put("label", "application/json");
        jds.put("yaxis", new Long(1) );
        jds.put("values", jBuckets.values() );
        // views
        Map<String, Object>vds = new HashMap<String,Object>();
        vds.put("label", "text/html");
        vds.put("yaxis", new Long(1) );
        vds.put("values", vBuckets.values() );
        Map<String, Object> envelope = new HashMap<String, Object>();
        envelope.put("json", jds );
        envelope.put("view", vds );
        envelope.put("errors", errors );
        envelope.put("clients", lclients );
        envelope.put("pageStats",  pageStats );
        envelope.put("total", new Long( totalCount ) );
        // create average times
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
        averageJsonProcessingTimeDS.put("label", "application/json");
        averageJsonProcessingTimeDS.put("yaxis", new Long(1) );
        averageJsonProcessingTimeDS.put("values",  averageJsonProcessingTimes);
        envelope.put("averageJSONProcessingTime", averageJsonProcessingTimeDS );
        // 
        Map<String, Object>averageJsonPayloadDS = new HashMap<String,Object>();
        averageJsonPayloadDS.put("label", "application/json");
        averageJsonPayloadDS.put("yaxis", new Long(1) );
        averageJsonPayloadDS.put("values",  averageJsonPayloads);
 
        envelope.put("averageJSONPayload", averageJsonPayloadDS );
        return envelope;
    }

    public List<IStat> getErrors( long duration ) {
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
            if ( !stat.hasErrors() ) continue;
            if ( stat.getTimestamp() > threshold ) {
                cStats.add( stat );
                // since we go backwards don't iterate if we have a stat already greater
            } else {
                break;
            }
        }
        return cStats;
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
}
