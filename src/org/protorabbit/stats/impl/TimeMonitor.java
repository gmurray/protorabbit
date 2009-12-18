package org.protorabbit.stats.impl;

public class TimeMonitor extends Thread {

    private static final long  HOUR_AND_TEN_MINUTES = 60 * 1000 * 70;
    private StatsManager sman = null;

    public TimeMonitor( StatsManager sman) {
        this.sman = sman;
    }

    long lastMatch = 0;
    private boolean running = true;

    private boolean checkIfMatches( long time) {
        if ( time % 300000 < 30000) {
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
                lastMatch = now;
            }
            try {
                sleep( 30000 );
            } catch (InterruptedException e) {
            }
        }
    }

}
