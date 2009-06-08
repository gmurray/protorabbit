package org.protorabbit.profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Episode{

    private long timeStamp;
    private String clientId;
    private String uri;

    private List<Mark> starts = new ArrayList<Mark>();
    private Map<String, Measure> measures = new HashMap<String, Measure>();

    public Episode(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void setStarts(List<Mark> starts) {
        this.starts = starts;
    }

    public List<Mark> getStarts() {
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

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getUri() {
		return uri;
	}

}
