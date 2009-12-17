package org.protorabbit.stats.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
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

        public TimeChartItem( long timestamp ) {
           this.timestamp = timestamp;
        }
        
        public void setValue( double value ) {
            this.value = value;
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
                Long bucketId = getRoundTime( resolution, stat.getTimestamp() );
                if (TEXT_HTML.equals(stat.getContentType()) ) {
                    TimeChartItem tci = vBuckets.get( bucketId );
                    if ( tci == null) {
                        tci = new TimeChartItem( bucketId );
                        vBuckets.put( bucketId, tci  );
                    }
                    tci.incrementValue();
                } else if (APPLICATION_JSON.equals(stat.getContentType()) ) {
                    TimeChartItem tci = jBuckets.get( bucketId );
                    if ( tci == null) {
                        tci = new TimeChartItem( bucketId );
                        jBuckets.put( bucketId, tci  );
                    }
                    tci.incrementValue();
                }
                // since we go backwards don't iterate if we have a stat already greater
            } else {
                break;
            }
        }
        //
        Map<String, Object>jds = new HashMap<String,Object>();
        jds.put("label", "JSON");
        jds.put("yaxis", new Long(1) );
        jds.put("values", jBuckets.values() );
        // views
        Map<String, Object>vds = new HashMap<String,Object>();
        vds.put("label", "text/html requests");
        vds.put("yaxis", new Long(1) );
        vds.put("values", vBuckets.values() );
        Map<String, Object> envelope = new HashMap<String, Object>();
        envelope.put("json", jds );
        envelope.put("view", vds );
        envelope.put("errors", errors );
        envelope.put("clients", lclients );
        envelope.put("pageStats",  pageStats );
        envelope.put("total", new Long( totalCount ) );
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
