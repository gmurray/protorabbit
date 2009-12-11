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

    public List<IStat> getStats() {
        return stats;
    }

    public synchronized void add( IStat s ) {
        if ( stats == null) {
            stats = new ArrayList<IStat>();
        }
        stats.add( s );
    }
}
