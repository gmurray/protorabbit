package org.protorabbit.stats.impl;

import java.util.logging.Logger;

public class TimeMonitor extends Thread {

    private static final long  HOUR_AND_TEN_MINUTES = 60 * 1000 * 70;
    private StatsManager sman = null;
    private static Logger logger = null;
    private boolean running = true;

    static final Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("org.protrabbit");
        }
        return logger;
    }

    public TimeMonitor( StatsManager sman) {
        this.sman = sman;
        super.setPriority( sman.getMonitorPriority() );
        String priority = "";
        switch ( sman.getMonitorPriority() ) {
            case Thread.MAX_PRIORITY : priority = "MAX_PRIORITY";
                break;
            case Thread.MIN_PRIORITY : priority = "MIN_PRIORITY";
            break;
            case Thread.NORM_PRIORITY : priority = "NORM_PRIORITY";
            break;
            default : priority = sman.getMonitorPriority() + "";
        }
        getLogger().info( "Starting time monitor with thread priority of " + priority );
    }

    /*
     * Prevent timer creep
     */
    private boolean checkIfMatches( long time) {
        if ( time % StatsManager.FIVE_MINUTES_MILLIS < StatsManager.THIRTY_SECOND_MILLIS ) {
            return true;
        }
        return false;
    }

    public void shutdown() {
        this.running = false;
        this.interrupt();
    }

    public void run() {
        while (running) {
            long now = System.currentTimeMillis();
            if ( checkIfMatches(now) ) {
                sman.pruneHistory( System.currentTimeMillis() - HOUR_AND_TEN_MINUTES );
            }
            try {
                sleep( StatsManager.THIRTY_SECOND_MILLIS );
            } catch (InterruptedException e) {
            }
        }
    }

}
