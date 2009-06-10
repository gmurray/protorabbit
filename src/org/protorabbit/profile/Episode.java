package org.protorabbit.profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Episode {

    private MarkComparator mc = null;
    private long timeStamp;
    private String clientId;
    private String uri;
    private String userAgent;

    private List<Mark> starts = new ArrayList<Mark>();
    private Map<String, Measure> measures = new HashMap<String, Measure>();

    public Episode(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void setStarts(List<Mark> starts) {
        this.starts = starts;
    }

    @SuppressWarnings("unchecked")
    class MarkComparator implements Comparator {

        public int compare(Object o1, Object o2){
            long t1 = ( (Mark) o1).getStartTime();
            long t2 = ( (Mark) o2).getStartTime();
            if ( t1 > t2 ) {
            return 1;
            } else if( t1 < t2 ) {
            return -1;
            } else {
                return 0;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public List<Mark> getStarts() {
        if (mc == null) {
            mc = new MarkComparator();
        }
        Collections.sort(starts, mc);
        return starts;
    }
    
    public void addStart(Mark m) {
        starts.add(m);
    }

    public void addMeasure(String id, Measure m) {
        measures.put(id, m);
    }

    public void setMeasures(Map<String, Measure> measures) {
        this.measures = measures;
    }

    public Map<String, Measure> getMeasures() {
        return measures;
    }

    public long getTimestamp() {
        return timeStamp;
    }

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientId() {
		return clientId;
	}
	
	public String getUserAgent() {
	    return userAgent;
	}
	
	public void setUserAgent(String userAgent) {
	    this.userAgent = userAgent;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getUri() {
		return uri;
	}

}
