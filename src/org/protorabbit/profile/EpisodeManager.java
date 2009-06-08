package org.protorabbit.profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class EpisodeManager {

    private List<Episode> episodes;
    private MarkComparator mc = null;

    public EpisodeManager() {
        reset();
    }

    public void reset() {
        episodes = new ArrayList<Episode>();
    }

    public List<Episode> getEpisodes() {
        return episodes;
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

    public Episode getEpisode(String clientId, long timestamp) {
        if (episodes == null) {
            return null;
        }
        for (Episode e : episodes) {
            if (e.getClientId().equals(clientId) &&
                e.getTimestamp() == timestamp) {
                return e;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public void addEpisode(String clientId, long timestamp, JSONObject json) {
        Episode e = getEpisode(clientId, timestamp);
        if (e == null) {
            e = new Episode(timestamp);
        }
        String uri = null;
        try {
            uri = json.getString("uri");
            JSONObject starts = json.getJSONObject("starts");
            Iterator<String> keys = starts.keys();
            List<Mark> marks = new ArrayList<Mark>();
            while (keys.hasNext()) {
                String key = keys.next();
                long timeStamp = starts.getLong(key);
                marks.add(new Mark(key, timeStamp));
                
            }
            if (mc == null) {
                mc = new MarkComparator();
            }
            Collections.sort(marks, mc);
            JSONObject jmeasures = json.getJSONObject("measures");
            keys = jmeasures.keys();
            Map<String,Measure> measures = new HashMap<String,Measure>();
            while (keys.hasNext()) {
                String key = keys.next();
                long duration = jmeasures.getLong(key);
                measures.put(key, new Measure(key, duration));
            }
            e.setUri(uri);
            e.setClientId(clientId);
            e.setMeasures(measures);
            e.setStarts(marks);
            episodes.add(e);
        } catch (JSONException e1) {
            e1.printStackTrace();
        }
    }
}
