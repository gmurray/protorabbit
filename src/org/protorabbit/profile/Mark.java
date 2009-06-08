package org.protorabbit.profile;

public class Mark {

    String id;
    long startTime = 0;

    public Mark(String id, long time) {
        this.startTime = time;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public long getStartTime() {
        return startTime;
    }
}
