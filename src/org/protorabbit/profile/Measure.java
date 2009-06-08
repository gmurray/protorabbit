package org.protorabbit.profile;

public class Measure {

    private String id;
    private long duration = 0;

    public Measure(String id, long time) {
        this.id = id;
        this.duration = time;
    }

    public String getId() {
        return id;
    }

    public long getDuration() {
        return duration;
    }
}
