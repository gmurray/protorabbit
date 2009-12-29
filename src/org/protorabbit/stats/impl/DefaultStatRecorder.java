package org.protorabbit.stats.impl;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.protorabbit.json.JSONSerializer;
import org.protorabbit.json.SerializationFactory;
import org.protorabbit.stats.IStat;
import org.protorabbit.stats.IStatRecorder;
import org.protorabbit.stats.impl.StatsManager.TimeChartItem;
import org.protorabbit.util.IOUtil;

public class DefaultStatRecorder implements IStatRecorder {

    private static final long ONE_DAY = 1000 * 60 * 60 * 24;
    private static final long FIVE_DAYS = ONE_DAY * 7;
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
        if ( now != today) {
            // if we already have an open writer close it
            if ( writer != null ) {
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
                return writer;
            } catch (IOException e) {
                getLogger().log( Level.SEVERE, "Could not create protorabbit directory to record stats." );
                canRecordStats = false;
                return null; 
            }
        }
        return null;
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

        Date d = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime( d );
        String hName = host + "." +
                       c.get( Calendar.YEAR) + "-" +
                       (c.get( Calendar.MONTH ) + 1) + "-" +
                       c.get( Calendar.DAY_OF_MONTH ) + ".json";
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
            if ( writer != null) {
                out.write( json.serialize(stat).toString() + "," );
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

    @SuppressWarnings("unchecked")
    public Map<String, Object> loadStats( File f ) {
        FileInputStream fis;
        Map<String, Object> stats = null;
        try {
            fis = new FileInputStream(f);
            StringBuffer buff = IOUtil.loadStringFromInputStream(fis, "UTF-8");
            // turn the buffer into an array
            buff.insert(0, "[");
            // remove the last comma
            buff.replace(buff.length() -1, buff.length(), "" );
            buff.append("]");
            List<IStat> items = (List<IStat>) json.deSerialize( buff.toString(), StatsItem.class );
            stats = StatsManager.getStats( null, StatsManager.Resolution.MINUTE, items );

        } catch (FileNotFoundException e) {
            getLogger().log( Level.SEVERE, "Could deserialize file.", e );
        } catch (IOException e) {
            getLogger().log( Level.SEVERE, "Could deserialize file.", e );
        }

        return stats;

    }

    public Map<String, Object> loadStatsForDate( long timestamp ) {
        if (statsDirectory != null) {

            File[] files = statsDirectory.listFiles();

            if (files != null && files.length > 0) {

                for (File f : files) {

                    String filename = f.getName();
                    Long ts = getFileTimestamp(host, filename);
                    if (ts != null && ts.longValue() == timestamp) {
                        return loadStats(f);
                    }
                }
            }
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private void combineLists(  Map<String, Object> src, Map<String, Object> target, String key ) {
        Map<String,Object> sViews = (Map<String,Object>)src.get(key);
        List<Object> sItems = (List<Object>) sViews.get("values");
        Map<String,Object> tViews = (Map<String,Object>)target.get(key);
        List<Object> tItems = (List<Object>) tViews.get("values");
        if ( tItems == null ) {
            tItems = new ArrayList<Object>();
            target.put(key, tItems );
        }
        if ( sItems != null ) {
            tItems.addAll( sItems );
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

    private void  processpageStats( Map<String,Object>sjson, Map<String,Object>tjson ) {

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
            processpageStats( sjson, tjson );
            Map<String,Object>tview = (Map<String,Object>)tPageStats.get( StatsManager.TEXT_HTML);
            Map<String,Object>sview = (Map<String,Object>)sPageStats.get( StatsManager.TEXT_HTML );
            if (tview == null ) {
                tview = new HashMap<String,Object>();
                tPageStats.put( StatsManager.TEXT_HTML.toString(), tview );
            }
            processpageStats( sview, tview );
        }
    }

    private void combineSummaries( Map<String, Object> src, Map<String, Object> target ) {
        // append views->values
        combineLists(src,target, "view");
        // append json->values
        combineLists(src,target, "json");
        // averageJSONProcessingTime->values
        combineLists(src,target, "averageViewProcessingTime");
        // averageJSONPayload->values
        combineLists(src,target, "averageViewPayload");
        // averageJSONProcessingTime->values
        combineLists(src,target, "averageJSONProcessingTime");
        // averageJSONPayload->values
        combineLists(src,target, "averageJSONPayload");
        // pageStats->text/html pageStats->application/json (in each get key work out averageContentLength, averageProcessingTime, accessCount, totalProcessingTime, totalContentLength
        combinePageStats( src, target );
    }

    public Object loadSummarySinceDate( long timestamp ) {
        Map<String, Object> summaries = null;
        if (statsDirectory != null) {

            List<Long> times = getSummaryTimestamps();

            if ( times.size() > 0) {

                for (Long l : times) {
                    if (l.longValue() >= timestamp ) {
                        if (summaries == null) {
                            summaries = (Map<String, Object>) loadSummaryForDate( l );
                        } else {
                            combineSummaries( (Map<String, Object>)loadSummaryForDate( l ), summaries );
                        }
                    }
                }
            }
        }
        return summaries;
    }

    public Object loadSummaryForDate( long timestamp ) {
        if (statsDirectory != null) {

            File[] files = statsDirectory.listFiles();

            if (files != null && files.length > 0) {

                for (File f : files) {

                    String filename = f.getName();
                    Long ts = getSummaryFileTimestamp(host, filename);
                    if (ts != null && ts.longValue() == timestamp) {
                        System.out.println("adding " + filename );
                        return loadSummary(f);
                    } else {
                  //      System.out.println("skipping " + filename + " ts=" + ts );
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

    public void cleanup() {
        getArchiveTimestamps();
        getSummaryTimestamps();
        if ( statsDirectory != null ) {

            File[] files = statsDirectory.listFiles();
            long removeCutoff = System.currentTimeMillis() - FIVE_DAYS;
            //long cutoff = System.currentTimeMillis() - ONE_DAY;
            long cutoff = System.currentTimeMillis() - 1000;
            if ( files != null && files.length > 0 ) {

                for ( File f : files ) {

                    String filename = f.getName();
                    Long ts = getFileTimestamp( host, filename );
                    if ( ts != null && ts.longValue() < cutoff ) {
                        try {

                            if ( filename.startsWith( host ) &&
                                    filename.endsWith(".json") &&
                                 !filename.endsWith("summary.json") ) {
                                try {
                                    
                                    String fName = "";
                                    int last = filename.lastIndexOf(".json");
                                    fName = filename.substring(0, last) + "-summary.json";
                                    File condensed = new File( statsDirectory, fName );
                                    getLogger().info("Condensing log " + filename + " to " + fName );
                                    Map<String, Object> stats = loadStats( f );
  
                                    OutputStream fos = new FileOutputStream(condensed);
                                    // serialize
                                    Object serialized = json.serialize( stats );
                                    ByteArrayInputStream bis = new ByteArrayInputStream(serialized.toString().getBytes("UTF-8"));
                                    IOUtil.saveToInputStream( bis , fos);
                                } catch (java.lang.OutOfMemoryError e) {
                                    getLogger().log( Level.SEVERE, "Out of Memory Error archiving stats file " + filename + ". Increase the heapspace to prevent future errors." );
                                } catch (Exception e) {
                                    getLogger().log( Level.INFO, "Error archiving  stats file " + filename );
                                }
                            } else {
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