package org.protorabbit.stats.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.protorabbit.json.JSONSerializer;
import org.protorabbit.json.SerializationFactory;
import org.protorabbit.stats.IClient;
import org.protorabbit.stats.IResourceStat;
import org.protorabbit.stats.IStat;
import org.protorabbit.stats.IStatRecorder;
import org.protorabbit.stats.impl.StatsManager.Resolution;
import org.protorabbit.stats.impl.StatsManager.TimeChartItem;
import org.protorabbit.util.IOUtil;

public class DefaultStatRecorder implements IStatRecorder {

    private static final long ONE_HOUR = 1000 * 60 * 60 ;
    private static final long ONE_DAY = ONE_HOUR * 24;
    private static final long SEVEN_DAYS = ONE_DAY * 7;
    private boolean canRecordStats = true;
    private static Logger logger = null;
    private File statsDirectory = null;
    private String host = "localhost";
    private JSONSerializer json = null;
    private long today = -1;
    private BufferedWriter writer = null;
    public static DateFormat df = new SimpleDateFormat("yyy-MM-dd");

    static final Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("org.protrabbit");
        }
        return logger;
    }

    public long getTime( long timestamp, long resolution ) {
        long mod = timestamp % ONE_DAY;
        return timestamp - mod;
    }

    public void init( ServletContext ctx ) {
        File temp = (File)ctx.getAttribute("javax.servlet.context.tempdir");

        if ( temp.isDirectory() ) {
            File pdir = new File ( temp, "protorabbit" );
            statsDirectory = new File ( pdir , "stats" );
            // create the stats directory if possible
            if ( !statsDirectory.exists() ) {
                boolean created = statsDirectory.mkdirs();
                if ( !created ) {
                    getLogger().log( Level.SEVERE, "Could not create protorabbit directory to record stats." );
                    canRecordStats = false;
                    return;
                }
            }
            getLogger().info( "Stats work area is : " + statsDirectory );

        } else {
            getLogger().info( "Could not access to work area." );
            canRecordStats = false;
        }
        try {
            InetAddress ia = null;
            ia = InetAddress.getLocalHost();
            host = ia.getHostName();
        } catch (UnknownHostException e) {
            getLogger().log( Level.WARNING, "Could not resolve host name. Defaulting to localhost." );
        }
        SerializationFactory factory = new SerializationFactory();
        json = factory.getInstance();
    }

    private BufferedWriter getWriter() {

        long now = getTime( System.currentTimeMillis(), ONE_DAY );
        if ( now != today || writer == null) {
            // if we already have an open writer close it
            if ( now != today && writer != null ) {
                updateDailySummary();
                close();
            }
            File currentStatsFile = getCurrentStatsFile();
            if ( currentStatsFile == null) {
                getLogger().log( Level.SEVERE, "Could not record stats." );
                canRecordStats = false;
                return null;
            }
            try {
                writer = new BufferedWriter( new FileWriter( currentStatsFile, true ) );
                today = now;
                return writer;
            } catch (IOException e) {
                getLogger().log( Level.SEVERE, "Could not create protorabbit directory to record stats." );
                canRecordStats = false;
                return null; 
            }
        }
        return writer;
    }

    private void close() {
        if ( writer != null ) {
            try {
                writer.close();
            } catch (IOException e) {
                getLogger().log( Level.SEVERE, "Could not create protorabbit directory to record stats." );
            }
        }
    }

    private File getCurrentStatsFile() {

        String hName = getBaseFileName() + ".json";
        File currentStatsFile = new File( statsDirectory, hName );
        if ( !currentStatsFile.exists() ) {
            try {
                currentStatsFile.createNewFile();

            } catch (IOException e) {
                getLogger().log( Level.SEVERE, "Could not create protorabbit directory to record stats." );
                canRecordStats = false;
                return null; 
            }
        }
        return currentStatsFile;
    }

    public synchronized void recordStatItem(IStat stat) {
        if ( !canRecordStats ) {
            return;
        }
        try {

            BufferedWriter out = getWriter();
            if ( out != null) {
                out.write( json.serialize(stat).toString() + ",\n" );
                out.flush();
            } else {
                getLogger().log( Level.SEVERE, "Could not write to stats file. The writer was null." );
                canRecordStats = false;
                return;
            }
        } catch (IOException e) {
            getLogger().log( Level.SEVERE, "Could not write to stats file." );
            canRecordStats = false;
        }

    }

    public void shutdown() {
        close();
    }

    public List<IStat> loadStats( Date d ) {
        return null;
    }

    private String getBaseFileName() {
        Date d = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime( d );
        String fName = host + "." +
                       c.get( Calendar.YEAR) + "-" +
                       (c.get( Calendar.MONTH ) + 1) + "-" +
                       c.get( Calendar.DAY_OF_MONTH );
        return fName;
    }

    public void updateDailySummary() {
        long start = System.currentTimeMillis();
        getLogger().info("Updating daily summary.");
        String fileName = getBaseFileName() + "-summary.json";
        Map<String, Object> stats = loadStats( getCurrentStatsFile() );
        archiveStats(fileName, stats);
        long end = System.currentTimeMillis();
        getLogger().info("Daily summary complete. Time : " + ( end - start ) + "ms" );
    }

    public Object loadSummary( File f ) {
        FileInputStream fis;
        Object stats = null;
        try {
            fis = new FileInputStream(f);
            StringBuffer buff = IOUtil.loadStringFromInputStream(fis, "UTF-8");
            stats =  json.genericDeserialize( buff.toString() );
        } catch (FileNotFoundException e) {
            getLogger().log( Level.SEVERE, "Could deserialize file.", e );
        } catch (IOException e) {
            getLogger().log( Level.SEVERE, "Could deserialize file.", e );
        }

        return stats;
    }

    private File getStatsFileForDate( long timestamp ) {
        if (statsDirectory != null) {

            File[] files = statsDirectory.listFiles();

            if (files != null && files.length > 0) {

                for (File f : files) {

                    String filename = f.getName();
                    Long ts = getFileTimestamp(host, filename);
                    if (ts != null && ts.longValue() == timestamp) {
                        return f;
                    }
                }
            }
        }
        return null;
    }

    public Map<String, Object> loadStatsForDate( long timestamp ) {
        File f = getStatsFileForDate( timestamp );
        if (f != null) {
            return loadStats(f);
        } else {
            return null;
        }
    }

    public Double getDouble( Object v ) {
        if ( v instanceof Integer) {
           return new Double((Integer)v);
        } else if (v instanceof Double){
            return (Double)v;
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private void  processPageStats( Map<String,Object>sjson, Map<String,Object>tjson ) {
        if ( sjson == null ) {
            return;
        }
        Iterator<String> i = null;
        if (sjson != null) {
            i = sjson.keySet().iterator();
        }
        while ( i != null && i.hasNext() ) {
            String key = i.next();
            Map<String,Object>pstats = (Map<String,Object>)sjson.get(key);
            Map<String,Object>targetItemStats = (Map<String,Object>)tjson.get(key);
            Double averageContentLength = getDouble(pstats.get( "averageContentLength" ));

            Double combinedContentLength = averageContentLength;
            Double averageProcessingTime = getDouble( pstats.get( "averageProcessingTime") );
            Double combinedProcessingTime = averageProcessingTime;
            Integer accessCount = (Integer)pstats.get( "accessCount" );
            Integer combinedAccessCount = accessCount;
            Integer totalProcessingTime = (Integer)pstats.get( "totalProcessingTime" );
            Integer combinedTotalProcessingTime = totalProcessingTime;
            Integer totalContentLength = (Integer)pstats.get( "totalContentLength" );
            Integer combinedTotalContentLength = totalContentLength;
            // only combine if the target has the stats
            if (targetItemStats != null) {
                Double targetAverageContentLength = getDouble( targetItemStats.get( "averageContentLength" ));
                combinedContentLength = (averageContentLength + targetAverageContentLength) / 2;
                Double targetAverageProcessingTime = getDouble( targetItemStats.get( "averageProcessingTime" ) );
                combinedProcessingTime = (averageProcessingTime + targetAverageProcessingTime) / 2;
                Integer targetAccessCount = (Integer)targetItemStats.get( "accessCount" );
                combinedAccessCount += targetAccessCount;
                Integer targetTotalProcessingTime = (Integer)targetItemStats.get( "totalProcessingTime" );
                combinedTotalProcessingTime += targetTotalProcessingTime;
                Integer targetTotalContentLength = (Integer)targetItemStats.get( "totalContentLength" );
                combinedTotalContentLength += targetTotalContentLength;
            } else {
                targetItemStats = new HashMap<String,Object>();
            }
            targetItemStats.remove( "averageContentLength" );
            targetItemStats.put( "averageContentLength", combinedContentLength );
            targetItemStats.put( "averageProcessingTime", combinedProcessingTime );
            targetItemStats.put( "accessCount", combinedAccessCount );
            targetItemStats.put( "totalProcessingTime", combinedTotalProcessingTime );
            targetItemStats.put( "totalContentLength", combinedTotalContentLength );
        }
        // add clients
        Map<String,Object>sclients = (Map<String,Object>)sjson.get("clients");
        Map<String,Object>tclients = (Map<String,Object>)tjson.get("clients");
        if ( sclients != null ) {
            Iterator<String> ci = sclients.keySet().iterator();
            while ( ci.hasNext() ) {
                String key = ci.next();
                // combine if in both sets
                if ( tclients.containsKey(key) ) {
                    // "pollInterval":5000,"errorCount":0,"jSONRequestCount":0,"totalRequestCount":1,"clientId":"10.2.237.6","lastAccess":1262109919385,"viewRequestCount":1}
                    // average the pollInterval
                    Integer spi = (Integer) sclients.get( "pollInterval");
                    Integer tpi = (Integer) tclients.get( "pollInterval");
                    tclients.put( "pollInterval", ( (spi + tpi) / 2) );
                    // errorCount
                    combineValues( sclients, tclients, "errorCount" );
                    // jSONRequestCount
                    combineValues( sclients, tclients, "jSONRequestCount" );
                    // totalRequestCount
                    combineValues( sclients, tclients, "totalRequestCount" );
                    // viewRequestCount
                    combineValues( sclients, tclients, "viewRequestCount" );
                    // get the greater of the two for the last access
                    Long sLastAccess = (Long) sclients.get( "lastAccess" );
                    Long tLastAccess = (Long) tclients.get( "lastAccess" );
                    // replace the lastAccesss only if the source is greater
                    if ( sLastAccess > tLastAccess ) {
                        tclients.put( "lastAccess", sLastAccess );
                    }
                }
            }
        }
    }

    private void combineValues( Map<String,Object>sclients, Map<String,Object>tclients, String key ) {
        Integer sTotal = (Integer) sclients.get( key );
        Integer tTotal = (Integer) tclients.get( key );
        tclients.put( key , (sTotal + tTotal) );
    }

    // pageStats->text/html pageStats->application/json 
    // (in each get key work out averageContentLength, averageProcessingTime, accessCount, totalProcessingTime, totalContentLength
    @SuppressWarnings("unchecked")
    private void combinePageStats(  Map<String, Object> src, Map<String, Object> target ) {
        Map<String,Object> tPageStats = (Map<String,Object>)target.get("pageStats");
        Map<String,Object> sPageStats = (Map<String,Object>)src.get("pageStats");
        if ( sPageStats != null) {
            Map<String,Object>tjson = (Map<String,Object>)tPageStats.get( StatsManager.APPLICATION_JSON );
            Map<String,Object>sjson = (Map<String,Object>)sPageStats.get( StatsManager.APPLICATION_JSON ) ;
            if (tjson == null ) {
                tjson = new HashMap<String,Object>();
                tPageStats.put( StatsManager.APPLICATION_JSON.toString(), tjson );
            }
            processPageStats( sjson, tjson );
            Map<String,Object>tview = (Map<String,Object>)tPageStats.get( StatsManager.TEXT_HTML);
            Map<String,Object>sview = (Map<String,Object>)sPageStats.get( StatsManager.TEXT_HTML );
            if (tview == null ) {
                tview = new HashMap<String,Object>();
                tPageStats.put( StatsManager.TEXT_HTML.toString(), tview );
            }
            processPageStats( sview, tview );
        }
    }

    @SuppressWarnings("unchecked")
    private void combineLists(  Map<String, Object> src, Map<String, Object> target, String key, Resolution r ) {
        List<Object> tItems = null;
        Map<String,Object> tViews = null;
        try {
            Map<String,Object> sViews = (Map<String,Object>)src.get(key);
            List<Object> sItems = (List<Object>) sViews.get("values");
            tViews = (Map<String,Object>)target.get(key);
            if (tViews != null ) {
                tItems = (List<Object>) tViews.get("values");
            }
            if ( tItems == null ) {
                tItems = new ArrayList<Object>();
                target.put(key, tItems );
            }
            if ( sItems != null ) {
                tItems.addAll( sItems );
            }
            tItems = condenseList( tItems, r.modValue() );
            tViews.put( "values", tItems );
        } catch ( Exception e ) {
            getLogger().log( Level.SEVERE, "Error combining lists.", e );
         }
    }
    
    @SuppressWarnings("unchecked")
    public List<Object> condenseList( List<Object> tItems, long r ) {
        LinkedHashMap<Long,Object> jBuckets = new LinkedHashMap<Long,Object>();
        for ( Object m : tItems ) {
            Map ti = (Map)m;
            Long timestamp = ( (Long)ti.get("time")).longValue();
            Long bucketId = StatsManager.getRoundTime( r, timestamp );
            // if we have the bucket item combine the value, otherwise the item becomes the first.
            if ( jBuckets.containsKey( bucketId ) ) {
                Map bti = (Map)jBuckets.get( bucketId );
                bti.put( bucketId, getDouble(bti.get("y")) + getDouble(ti.get("y")) );
            } else {
                jBuckets.put( bucketId, ti );
            }
        }
        List<Object> rvals = new ArrayList();
        rvals.addAll(jBuckets.values());
        return rvals;
    }
    
    private void combineSummaries( Map<String, Object> src, Map<String, Object> target, Resolution r ) {
        // append views->values
        combineLists(src,target, "view", r);
        // append json->values
        combineLists(src,target, "json", r);
        // averageJSONProcessingTime->values
        combineLists(src,target, "averageViewProcessingTime", r);
        // averageJSONPayload->values
        combineLists(src,target, "averageViewPayload", r);
        // averageJSONProcessingTime->values
        combineLists(src,target, "averageJSONProcessingTime", r);
        // averageJSONPayload->values
        combineLists(src,target, "averageJSONPayload", r);
        // pageStats->text/html pageStats->application/json (in each get key work out averageContentLength, averageProcessingTime, accessCount, totalProcessingTime, totalContentLength
        combinePageStats( src, target );
    }

    @SuppressWarnings("unchecked")
    public Object loadSummarySinceDate( long timestamp, Resolution r ) {
        Map<String, Object> summaries = null;
        if (statsDirectory != null) {

            List<Long> times = getSummaryTimestamps();

            Collections.sort( times );

            if ( times.size() > 0) {

                for (Long l : times) {
                    if (l.longValue() >= timestamp ) {
                        if (summaries == null) {
                            summaries = (Map<String, Object>) loadSummaryForDate( l );
                        } else {
                            combineSummaries( (Map<String, Object>)loadSummaryForDate( l ), summaries, r );
                        }
                    }
                }
            }
        }
        return summaries;
    }

    private long getDay( long timestamp ) {
        Date d = new Date();
        d.setTime( timestamp );
        Calendar c = Calendar.getInstance();
        c.setTime( d );
        Calendar rc = Calendar.getInstance();
        rc.set( Calendar.YEAR, c.get(Calendar.YEAR) );
        rc.set( Calendar.DATE, c.get(Calendar.DATE) );
        rc.set( Calendar.SECOND, 0 );
        rc.set( Calendar.MINUTE, 0 );
        rc.set( Calendar.HOUR_OF_DAY , 0 );
        rc.set( Calendar.MILLISECOND, 0 );
        return rc.getTimeInMillis();
    }

    /*
     * Find a range of detailed stats (Resolution.SECOND)
     */
    public Object loadArchivedStatsForRange( long statTimestamp, long endTimestamp, Resolution r ) {
        if (statsDirectory == null) { 
            return null;
        }

        Long duration = null;
        long resolution = r.modValue();
        Map<Long,TimeChartItem> vBuckets = new LinkedHashMap<Long,TimeChartItem>();
        Map<Long,TimeChartItem> jBuckets = new LinkedHashMap<Long,TimeChartItem>();

        Map<String,IClient> lclients = new HashMap<String,IClient>();
        Map<String, Map<String,IResourceStat>> pageStats = new Hashtable<String, Map<String,IResourceStat>>();

        List<IStat> errors = new ArrayList<IStat>();
        long threshold = -1;
        if (duration != null) {
            threshold = System.currentTimeMillis() - duration;
        }
        Integer totalCount = 0;

        List<Long> times = getArchiveTimestamps();

        Collections.sort( times );
        List<IStat> items = new ArrayList<IStat>();
        getLogger().info( "Loading stats for date range " + statTimestamp + " to " + endTimestamp + " file count is " + times.size() + " files are " + times );
        if ( times.size() > 0) {

            for (Long l : times) {
                long startDay =getDay( statTimestamp );
                long endDay = getDay( endTimestamp );
                getLogger().info( "Start day=" + startDay + " endDay=" + endDay );

                // look at stats file and add a day to make sure partials get picked up
                if ( (startDay == l.longValue()) || (l.longValue() == endDay) ) {
 
                    try { 
                        FileReader fis = null;
                        File f = getStatsFileForDate( l.longValue() );
                        getLogger().info( "Loading starts from " + f.getName() );
                        fis = new FileReader(f);
                        // read 1000 lines and process

                            BufferedReader in = new BufferedReader( fis );
                            StringBuffer buff = new StringBuffer();
                            String line = null;
                            int lineCount = 0;
                            while ((line = in.readLine()) != null) {
                                buff.append( line );
                                lineCount++;
                                if ( lineCount % 1000 == 0) {
                                    List<IStat> litems = processBuffer( buff );
                                    for ( IStat i : litems ) {
                                        if ( i.getTimestamp() >= statTimestamp && i.getTimestamp() <= endTimestamp) {
                                            items.add( i );
                                            totalCount++;
                                        }
                                    }
                                    buff = new StringBuffer();
                                }
                            }
                            // cleanup the rest of the buffer
                            if ( buff.length() > 0 ) {
                                List<IStat> litems = processBuffer( buff );
                                for ( IStat i : litems ) {
                                    if ( i.getTimestamp() >= statTimestamp && i.getTimestamp() <= endTimestamp) {
                                        items.add( i );
                                        totalCount++;
                                    }
                                }
                                StatsManager.addStats( duration, resolution, items, vBuckets, jBuckets,
                                        lclients, pageStats,  errors,
                                        threshold ) ;
                            }
                            getLogger().info("Read in " + f.getName() + " lines : " + lineCount + " total stat count : " + totalCount );
                            in.close();

                    } catch (FileNotFoundException e) {
                        getLogger().log( Level.SEVERE, "Could deserialize file.", e );
                    } catch (IOException e) {
                        getLogger().log( Level.SEVERE, "Could deserialize file.", e );
                    }
                }
            }
        }
        StatsManager.addStats( duration, resolution, items, vBuckets, jBuckets,
                lclients, pageStats, errors,
                threshold ) ;
      //  return summaries;
        return StatsManager.createStatsEnvelope( vBuckets, jBuckets,
                lclients, pageStats,  errors,
                threshold,  totalCount );
    }
    
    @SuppressWarnings({ "unchecked" })
    private List<IStat> processBuffer( StringBuffer buff ) {
        buff.insert(0, "[");
        // remove the last comma
        buff.replace(buff.length() -1, buff.length(), "" );
        buff.append("]");
        List<IStat> items = (List<IStat>) json.deSerialize( buff.toString(), StatsItem.class );
       return items;
    }

    public Map<String, Object> loadStats( File f ) {
        Long duration = null;
        long resolution = Resolution.MINUTE.modValue();
        Map<Long,TimeChartItem> vBuckets = new LinkedHashMap<Long,TimeChartItem>();
        Map<Long,TimeChartItem> jBuckets = new LinkedHashMap<Long,TimeChartItem>();

        Map<String,IClient> lclients = new HashMap<String,IClient>();
        Map<String, Map<String,IResourceStat>> pageStats = new Hashtable<String, Map<String,IResourceStat>>();

        List<IStat> errors = new ArrayList<IStat>();
        long threshold = -1;
        if (duration != null) {
            threshold = System.currentTimeMillis() - duration;
        }
        Integer totalCount = 0;
        FileReader fis;

        try {
            fis = new FileReader(f);
            // read 1000 lines and process

                BufferedReader in = new BufferedReader( fis );
                StringBuffer buff = new StringBuffer();
                String line = null;
                int lineCount = 0;
                while ((line = in.readLine()) != null) {
                    buff.append( line );
                    lineCount++;
                    if ( lineCount % 1000 == 0) {
                        List<IStat> items = processBuffer( buff );
                        totalCount += items.size();
                        StatsManager.addStats( duration, resolution, items, vBuckets, jBuckets,
                                lclients, pageStats, errors,
                                threshold ) ;
                        buff = new StringBuffer();
                    }
                }
                // cleanup the rest of the buffer
                if ( buff.length() > 0 ) {
                    List<IStat> items = processBuffer( buff );
                    totalCount += items.size();
                    StatsManager.addStats( duration, resolution, items, vBuckets, jBuckets,
                            lclients, pageStats,  errors,
                            threshold ) ;
                }
                getLogger().info("Read in " + f.getName() + " lines : " + lineCount + " total stat count : " + totalCount );
                in.close();

                return StatsManager.createStatsEnvelope( vBuckets, jBuckets,
                        lclients, pageStats,  errors,
                        threshold,  totalCount );
        } catch (FileNotFoundException e) {
            getLogger().log( Level.SEVERE, "Could deserialize file.", e );
        } catch (IOException e) {
            getLogger().log( Level.SEVERE, "Could deserialize file.", e );
        }

        return null;

    }

    public Object loadSummaryForDate( long timestamp ) {
        if (statsDirectory != null) {

            File[] files = statsDirectory.listFiles();

            if (files != null && files.length > 0) {

                for (File f : files) {

                    String filename = f.getName();
                    Long ts = getSummaryFileTimestamp(host, filename);
                    if (ts != null && ts.longValue() == timestamp) {
                        getLogger().info("Summary adding : " + filename );
                        return loadSummary(f);
                    }
                }
            }
        }
        return null;
    }

    public static Long getSummaryFileTimestamp( String host, String filename) {
        try {
            if ( filename.startsWith( host ) &&
                 filename.endsWith("-summary.json") ) {
                int start = host.length() + 1;
                int stop = filename.lastIndexOf("-summary.json");
                String dateString = filename.substring(start, stop);
                return new Long( df.parse( dateString).getTime() );
            }
        } catch (Exception e) {
            getLogger().log( Level.WARNING, "Could not get summray filename for file ." + filename, e );
        }
        return null;
    }

    public static Long getFileTimestamp( String host, String filename) {
        try {

            if ( filename.startsWith( host ) &&
                    filename.endsWith(".json") && 
                   !filename.endsWith("-summary.json") ) {
                int start = host.length() + 1;
                int stop = filename.lastIndexOf(".json");
                String dateString = filename.substring(start, stop);
                return new Long( df.parse( dateString).getTime() );
                
            }
        } catch (Exception e) {
            getLogger().log( Level.WARNING, "Could not get filename for file ." + filename, e );
        }
        return null;
    }

    public List<Long> getSummaryTimestamps() {
        List<Long> fileNames = new ArrayList<Long>();
        if ( statsDirectory != null ) {
 
            File[] files = statsDirectory.listFiles();
            if ( files != null && files.length > 0 ) {
                for ( File f : files ) {
                    Long ts = getSummaryFileTimestamp( host, f.getName());
                    if ( ts != null ) {
                        fileNames.add( ts );
                    }
                }
            }
        }
        return fileNames;
    }

    public List<Long> getArchiveTimestamps() {
        List<Long> fileNames = new ArrayList<Long>();
        if ( statsDirectory != null ) {
 
            File[] files = statsDirectory.listFiles();
            if ( files != null && files.length > 0 ) {
                for ( File f : files ) {
                    Long ts = getFileTimestamp( host, f.getName());
                    if ( ts != null ) {
                        fileNames.add( ts );
                    }
                }
            }
        }
        return fileNames;
    }

    public void archiveStats( String fName, Map<String, Object> stats ) {
        try {
            File condensed = new File( statsDirectory, fName );
            OutputStream fos = new FileOutputStream(condensed);
            // serialize
            Object serialized = json.serialize( stats );
            ByteArrayInputStream bis = new ByteArrayInputStream(serialized.toString().getBytes("UTF-8"));
            IOUtil.saveToInputStream( bis , fos);
        } catch (java.lang.OutOfMemoryError e) {
            getLogger().log( Level.SEVERE, "Out of Memory Error archiving stats file " + fName + ". Increase the heapspace to prevent future errors." );
        } catch (Exception e) {
            getLogger().log( Level.INFO, "Error archiving  stats file " + fName );
        }
    }

    public void cleanup() {

        if ( statsDirectory != null ) {

            File[] files = statsDirectory.listFiles();
            long removeCutoff = System.currentTimeMillis() - SEVEN_DAYS;
            long cutoff = System.currentTimeMillis() - ONE_DAY;
            if ( files != null && files.length > 0 ) {

                for ( File f : files ) {

                    String filename = f.getName();
                    Long ts = getFileTimestamp( host, filename );
                    if ( ts != null && ts.longValue() < cutoff ) {
                        try {

                            if ( filename.startsWith( host ) &&
                                    filename.endsWith(".json") &&
                                 !filename.endsWith("summary.json") ) {

                                if ( ts.longValue() < removeCutoff ) {
                                    getLogger().log( Level.INFO, "Removing stats file ." + filename );
                                    f.delete();
                                } else {
                                   getLogger().info("Skipping " + filename);
                                }
                            }
                        } catch (Exception e) {
                            getLogger().log( Level.SEVERE, "Could not write to stats file.", e );
                        }
                    }
                }
            }
        }
    }

    public boolean canRecordStats() {
        return canRecordStats;
    }

}
