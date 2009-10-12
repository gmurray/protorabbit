package org.protorabbit.communicator;

public class Message {

    private String id;
    private Object data;
    private String topic;

    public Message(String topic, Object data) {
        this.topic = topic;
        this.data = data;
    }

    public Message(String id, String topic, Object data) {
        this.id = id;
        this.topic = topic;
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

}
