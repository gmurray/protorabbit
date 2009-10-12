package org.protorabbit.communicator;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

public class PollManager {

    private ServletContext ctx;
    public static final String POLL_MANAGER = "org.protorabbit.communicator.POLL_MANAGER";
    private static final String JSON_POLLERS = "org.protorabbit.communicator.JSON_POLLERS";
    private static final String JSON_POLLER_CLEANUP_TIMEOUT = "org.protorabbit.communicator.JSON_POLLER_CLEANUP_TIMEOUT";

    // timeout for pollerCleanup
    private long pollerCleanupTimeout = 60 * 1000 * 1;
    // default poll Interval in mills
    private Long defaultPollInterval = 5000L;

    // timeout the pollers if they are greater than 30 seconds
    private long pollerTimeout = 1000 * 30;

    public PollManager(ServletContext ctx) {
        this.ctx = ctx;
        ctx.setAttribute(POLL_MANAGER, this);
    }

    private static Logger logger = null;

    static final Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("org.protrabbit");
        }
        return logger;
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

        Long lastCleanup = (Long)ctx.getAttribute(JSON_POLLER_CLEANUP_TIMEOUT);
        long now = (new Date()).getTime();

        Map<String,JSONPoller> pollers = (Map<String,JSONPoller>)ctx.getAttribute(JSON_POLLERS);
        if (pollers == null) {
            pollers = new HashMap<String,JSONPoller>();
            ctx.setAttribute(JSON_POLLERS,pollers);
        }
        // handle the current client
        String clientId = request.getRemoteAddr();
        JSONPoller poller = pollers.get(clientId);
        if (poller == null) {
            poller = new JSONPoller(clientId);
            poller.setPollInterval(defaultPollInterval);
            pollers.put(clientId, poller);
        }
        poller.incrementCount();
        // do poller cleanup every 5 minutes
        if (lastCleanup == null) {
            ctx.setAttribute(JSON_POLLER_CLEANUP_TIMEOUT, new Long(now));
        } else {
            if (now - lastCleanup.longValue() > pollerCleanupTimeout) {
                // cleanup
                Iterator<String> it = pollers.keySet().iterator();
                List<String> keysToRemove = new ArrayList();
                // we only want this block running once at a time
                // to prevent concurrent access exceptions
                synchronized(this) {
                    while (it.hasNext()) {
                        String key = it.next();
                        JSONPoller p = pollers.get(key);
                        if (p != null) {
                            if (now - p.getLastAccess() > pollerTimeout ) {
                                logger.log(Level.INFO, "Removing stale poller client " + p.getId());
                                keysToRemove.add(key);
                            }
                        }
                    }
                    for (String key : keysToRemove) {
                        pollers.remove(key);
                    }
                }
                // update the poller Intervals
                int clientCount = pollers.keySet().size();

                getLogger().log(Level.INFO, "Adjusting pollerCounts for " + clientCount + " clients.");
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
                    JSONPoller p = pollers.get(key);
                    if (p != null) {
                        p.setPollInterval(pollInterval);
                    }
                }
                ctx.setAttribute(JSON_POLLER_CLEANUP_TIMEOUT, new Long(now));
            }
        }

        return poller.getPollInterval();
    }
}
