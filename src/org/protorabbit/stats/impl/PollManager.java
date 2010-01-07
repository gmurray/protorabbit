package org.protorabbit.stats.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.protorabbit.stats.IClient;


public class PollManager {

    public static final String POLL_MANAGER = "org.protorabbit.communicator.POLL_MANAGER";
    private static PollManager pm = null;

    // timeout for pollerCleanup
    private long pollerCleanupTimeout = 60 * 1000 * 1;
    // default poll Interval in mills
    private Long defaultPollInterval = 5000L;

    // timeout the pollers if they are greater than 30 seconds
    private long pollerTimeout = 1000 * 30;
    private Map<String,IClient> pollers = null;
    Long lastCleanup = null;

    private static Logger logger = null;

    static final Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("org.protrabbit");
        }
        return logger;
    }

    private PollManager() {
    }

    public static PollManager getInstance() {
        if (pm == null) {
            pm = new PollManager();
        }
        return pm;
    }
    public Map<String,IClient> getPollers() {
        return pollers;
    }

    /*
     * Manage a set of pollers where the poll duration dynamically adjusts to the number of 
     * clients that are accessing the system.
     * 
     * This method will check the poller size every five minutes (JSON_POLLER_CLEANUP_TIMEOUT)
     *  and will also cleanout client that are not active within the last 2 hours (pollerCleanupTimeout).
     * 
     */
    @SuppressWarnings("unchecked")
    public Long getPollInterval(HttpServletRequest request) {

        long now = (new Date()).getTime();

        if (pollers == null) {
            pollers = new HashMap<String,IClient>();
        }
        // handle the current client
        String clientId = request.getRemoteAddr();
        IClient poller = pollers.get( clientId );
        if (poller == null) {
            poller = new Client(clientId);
            poller.setPollInterval( defaultPollInterval );
            pollers.put(clientId, poller);
        }
        poller.incrementPollCount();
        // do poller cleanup every 5 minutes
        if (lastCleanup == null) {
            lastCleanup =  new Long( now );
        } else {
            if (now - lastCleanup.longValue() > pollerCleanupTimeout) {
                // cleanup
                Iterator<String> it = pollers.keySet().iterator();
                List<String> keysToRemove = new ArrayList();
                // we only want this block running once at a time
                // to prevent concurrent access exceptions
                synchronized(this) {
                    try {
                    while (it.hasNext()) {
                        String key = it.next();
                        IClient p = pollers.get(key);
                        if (p != null ) {
                            if (now - p.getLastAccess() > pollerTimeout ) {
                                getLogger().log(Level.INFO, "Removing stale poller client " + p.getClientId() );
                                keysToRemove.add(key);
                            }
                        }
                    }
                    } catch (Exception e) {
                        getLogger().severe( e.getLocalizedMessage() );
                    }
                    for (String key : keysToRemove) {
                        pollers.remove(key);
                    }
                }
                // update the poller Intervals
                int clientCount = pollers.keySet().size();

                Long pollInterval = defaultPollInterval;
                if (clientCount > 20 && clientCount < 40) {
                    pollInterval += 4000;
                } else  if (clientCount >= 40 && clientCount < 60) {
                    pollInterval += 5000;
                } else if (clientCount > 60){
                    pollInterval += 8000;
                }

                Iterator<String> pit = pollers.keySet().iterator();

                while (pit.hasNext()) {
                    String key = pit.next();
                    IClient p = pollers.get(key);
                    if (p != null) {
                        p.setPollInterval(pollInterval);
                    }
                }

            }
        }
        poller.setLastAccess( System.currentTimeMillis() );
        return poller.getPollInterval();
    }
}
