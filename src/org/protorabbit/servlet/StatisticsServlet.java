package org.protorabbit.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.protorabbit.communicator.JSONResponse;
import org.protorabbit.json.JSONSerializer;
import org.protorabbit.json.SerializationFactory;
import org.protorabbit.stats.impl.StatsManager;
import org.protorabbit.stats.impl.StatsManager.Resolution;

public class StatisticsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected ServletContext ctx;

    private static Logger logger = null;
    private StatsManager statsManager = null;
    private JSONSerializer json = null;

    static final Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("org.protrabbit.communicator");
        }
        return logger;
    }

    public void init(ServletConfig cfg) {
        try {
            super.init(cfg);
            this.ctx = cfg.getServletContext();
            statsManager = (StatsManager)ctx.getAttribute(StatsManager.STATS_MANAGER);
            SerializationFactory factory = new SerializationFactory();
            json = factory.getInstance();
        } catch (ServletException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        String command = null;
        if ( command == null ) {
            command = request.getPathInfo();
        }
        if ("/current".equals(command) ) {
            String duration = request.getParameter( "duration" );
            int d = 60;
            if ( duration != null ) {
                try {
                d = Integer.parseInt( duration );
                } catch ( NumberFormatException nfe ) {
                    getLogger().warning( "Error with duration parameter : " + nfe.getMessage() );
                }
            }
            String r = request.getParameter( "resolution" );
            Resolution resolution = null;
            if ( r != null ) {
                try {
                    resolution = Resolution.valueOf( r );
                } catch ( Exception e) {
                    getLogger().warning( "Bad resolution " + r + ". Will use default." );
                }
            }
            if ( resolution == null ) {
                resolution = Resolution.SECOND;
            }
            Object data = null;
            data = statsManager.getLatest( 1000 * d, resolution );
            response.setHeader( "pragma", "NO-CACHE");
            response.setHeader( "Cache-Control", "no-cache" );
            Object jo = json.serialize( data );
            response.getWriter().write( jo.toString() );
            return;
        } else if (command.startsWith("/summary") ) {

            String dateString = getLastPathItem( command );

            long d = -1;
            if ( dateString != null ) {
                try {
                d = Long.parseLong( dateString );
                } catch ( NumberFormatException nfe ) {
                    getLogger().warning( "Error with timestamp parameter : " + nfe.getMessage() );
                }
            }
            Object data = null;
            data = statsManager.getSummaryForDate( d );
            response.setHeader( "pragma", "NO-CACHE");
            response.setHeader( "Cache-Control", "no-cache" );
            Object jo = json.serialize( data );
            response.getWriter().write( jo.toString() );
            return;
        } else if (command.startsWith("/stats") ) {

            String dateString = getLastPathItem( command );

            long d = -1;
            if ( dateString != null ) {
                try {
                    d = Long.parseLong( dateString );
                } catch ( NumberFormatException nfe ) {
                    getLogger().warning( "Error with timestamp parameter : " + nfe.getMessage() );
                }
            }
            Object data = null;
            data = statsManager.getStatsForDate( d );
            response.setHeader( "pragma", "NO-CACHE");
            response.setHeader( "Cache-Control", "no-cache" );
            Object jo = json.serialize( data );
            response.getWriter().write( jo.toString() );
            return;

        } else if ( "/archivedStats".equals( command ) ) {
                String startDateString = request.getParameter( "start" );
                long startTimestamp = -1;
                if ( startDateString != null ) {
                    try {
                        startTimestamp = Long.parseLong( startDateString );
                    } catch ( NumberFormatException nfe ) {
                        getLogger().warning( "Error with timestamp parameter : " + nfe.getMessage() );
                    }
                }
                String endDateString = request.getParameter( "end" );
                long endTimestamp = -1;
                if ( endDateString != null ) {
                    try {
                        endTimestamp = Long.parseLong( endDateString );
                    } catch ( NumberFormatException nfe ) {
                        getLogger().warning( "Error with timestamp parameter : " + nfe.getMessage() );
                    }
                }
                String r = request.getParameter( "resolution" );
                Resolution resolution = null;
                if ( r != null ) {
                    try {
                        resolution = Resolution.valueOf( r );
                    } catch ( Exception e) {
                        getLogger().warning( "Bad resolution " + r + ". Will use default." );
                    }
                }
                if ( resolution == null ) {
                    resolution = Resolution.MINUTE;
                }
                Object data = null;
                if ( startTimestamp != -1 && endTimestamp != -1) {
                    data = statsManager.loadArchivedStatsForRange( startTimestamp, endTimestamp, resolution );
                } else {
                    data = createJSONError( "A start and end timestamp is required." );
                }
                response.setHeader( "pragma", "NO-CACHE");
                response.setHeader( "Cache-Control", "no-cache" );
                Object jo = json.serialize( data );
                response.getWriter().write( jo.toString() );
                return;
        } else if ( "/archiveTimestamps".equals( command ) ) {

            Object data = null;
            data = statsManager.getArchiveTimestamps();
            response.setHeader( "pragma", "NO-CACHE");
            response.setHeader( "Cache-Control", "no-cache" );
            Object jo = json.serialize( data );
            response.getWriter().write( jo.toString() );
            return;
       } else if ( "/summaryTimestamps".equals( command ) ) {

                Object data = null;
                data = statsManager.getSummaryTimestamps();
                response.setHeader( "pragma", "NO-CACHE");
                response.setHeader( "Cache-Control", "no-cache" );
                Object jo = json.serialize( data );
                response.getWriter().write( jo.toString() );
                return;
       } else if ( command.startsWith("/summariesSinceDate") ) {
           String dateString = getLastPathItem( command );
           long d = -1;
           if ( dateString != null ) {
               try {
                   d = Long.parseLong( dateString );
               } catch ( NumberFormatException nfe ) {
                   getLogger().warning( "Error with timestamp parameter : " + nfe.getMessage() );
               }
           }
           Object data = null;
           data = statsManager.loadSummarySinceDate( d );
           response.setHeader( "pragma", "NO-CACHE");
           response.setHeader( "Cache-Control", "no-cache" );
           Object jo = json.serialize( data );
           response.getWriter().write( jo.toString() );
           return;
      }  else {
                String commands = "Commands are : all, " +
                "summary/[timestamp]," +
                "stats/[timestamp], " + 
                "archiveTimestamps, " + 
                "summaryTimestamps";
                Object data = createJSONError( commands );
                response.setHeader( "pragma", "NO-CACHE");
                response.setHeader( "Cache-Control", "no-cache" );
                Object jo = json.serialize( data );
                response.getWriter().write( jo.toString() );
        }
    }

    private Object createJSONError(String message) {
        JSONResponse jr = new JSONResponse();
        List<String> errors = new ArrayList<String>();
        errors.add( message );
        jr.setErrors(errors);

        return jr;
    }

    private static String getLastPathItem( String path ) {
        int last = path.lastIndexOf("/");
        if ( last != -1 ) {
            return path.substring( last + 1 );
        }
        return null;
    }

}