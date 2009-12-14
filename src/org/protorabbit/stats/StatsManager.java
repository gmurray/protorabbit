package org.protorabbit.stats;

import java.util.ArrayList;
import java.util.List;

public class StatsManager {

    private List<IStat> stats = null;
    private static StatsManager statManager = null;

    // prevent non use of constructor
    private StatsManager() {}

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

    public void add( IStat s ) {
        if ( stats == null) {
            stats = new ArrayList<IStat>();
        }
        synchronized(stats) {
            stats.add( s );
        }
    }
}
