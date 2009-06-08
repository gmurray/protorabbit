package org.protorabbit.profile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public class EpisodeManager {

    private List<Episode> episodes;

    public EpisodeManager() {
        reset();
    }

    public void reset() {
        episodes = new ArrayList<Episode>();
    }

    public List<Episode> getEpisodes() {
        return episodes;
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
           // List<Mark> marks = new ArrayList<Mark>();
            while (keys.hasNext()) {
                String key = keys.next();
                long timeStamp = starts.getLong(key);
                e.addStart(new Mark(key, timeStamp));
                
            }
            JSONObject jmeasures = json.getJSONObject("measures");
            keys = jmeasures.keys();
            //Map<String,Measure> measures = new HashMap<String,Measure>();
            while (keys.hasNext()) {
                String key = keys.next();
                long duration = jmeasures.getLong(key);
                e.addMeasure(key, new Measure(key, duration));
            }
            e.setUri(uri);
            e.setClientId(clientId);
          //  e.setMeasures(measures);
          //  e.setStarts(marks);
            episodes.add(e);
        } catch (JSONException e1) {
            e1.printStackTrace();
        }
    }
}
