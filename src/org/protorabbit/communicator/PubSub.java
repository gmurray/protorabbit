package org.protorabbit.communicator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.protorabbit.json.JSONSerializer;
import org.protorabbit.json.SerializationFactory;
import org.json.JSONArray;
import org.json.JSONObject;

public class PubSub {

    private static PubSub               singleton = null;

    ArrayList<Message>                  messages;

    HashMap<String, ArrayList<Handler>> handlerMappings;
    ArrayList<SpecialMapping>           specialMappings;
    JSONSerializer                      js        = null;

    protected PubSub() {

        messages = new ArrayList<Message>();
        handlerMappings = new HashMap<String, ArrayList<Handler>>();
        specialMappings = new ArrayList<SpecialMapping>();
        js = new SerializationFactory().getInstance();
    }

    public static PubSub getInstance() {
        if (singleton == null) {
            singleton = new PubSub();
        }
        return singleton;
    }

    public void publish(Message m) {
        // if we are in comet node do a direct publish
        messages.add(m);
    }

    void processHandler(String topic, JSONObject args, Handler h) {

        try {
            h.processRequest(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JSONArray getMessages() {
        // assumption here is that the messages is a Collection
        JSONArray ja = (JSONArray) js.serialize(messages);
        return ja;
    }

    public void clearMessages() {
        messages.clear();
    }

    /*
     * Process all the matching topic requests
     */
    public void processRequest(String topic, JSONObject args) {

        if (topic.indexOf("*") != -1) {
            Iterator<SpecialMapping> it = specialMappings.iterator();
            while (it.hasNext()) {
                SpecialMapping m = it.next();
                if (m.matches(topic)) {
                    processHandler(topic, args, m.getHandler());
                }
            }
        } else {
            ArrayList<Handler> handlers = handlerMappings.get(topic);
            if (handlers != null) {
                Iterator<Handler> it = handlers.iterator();
                while (it.hasNext()) {
                    Handler h = it.next();
                    processHandler(topic, args, h);
                }
            }
        }
    }

    public void subscribeRegex(String topic, Handler h) {
        specialMappings.add(new SpecialMapping(topic, h, true));
    }

    public void subscribe(String topic, Handler h) {
        if (topic.indexOf("*") != -1) {
            specialMappings.add(new SpecialMapping(topic, h, false));
        } else {
            ArrayList<Handler> handlers = handlerMappings.get(topic);
            if (handlers == null) {
                handlers = new ArrayList<Handler>();
                handlerMappings.put(topic, handlers);
            }
            handlers.add(h);
        }
    }

    boolean matchWildcard(String pattern, String topic) {

        int patpos = 0;
        int patlen = pattern.length();
        int strpos = 0;
        int strlen = topic.length();

        int i = 0;
        boolean star = false;

        while (strpos + i < strlen) {
            if (patpos + i < patlen) {
                switch (pattern.charAt(patpos + i)) {
                case '?':
                    i++;
                    continue;
                case '*':
                    star = true;
                    strpos += i;
                    patpos += i;
                    do {
                        ++patpos;
                        if (patpos == patlen) {
                            return true;
                        }
                    } while (pattern.charAt(patpos) == '*');
                    i = 0;
                    continue;
                }
                if (topic.charAt(strpos + i) != pattern.charAt(patpos + i)) {
                    if (!star) {
                        return false;
                    }
                    strpos++;
                    i = 0;
                    continue;
                }
                i++;
            } else {
                if (!star) {
                    return false;
                }
                strpos++;
                i = 0;
            }
        }
        do {
            if (patpos + i == patlen) {
                return true;
            }
        } while (pattern.charAt(patpos + i++) == '*');
        return false;
    };

    class SpecialMapping {

        Handler handler;
        String  topic;
        boolean isRegexp = false;

        public SpecialMapping(String topic, Handler handler, boolean isRegexp) {
            this.topic = topic;
            this.handler = handler;
            this.isRegexp = isRegexp;
        }

        public Handler getHandler() {
            return handler;
        }

        public String getTopic() {
            return topic;
        }

        public boolean matches(String test) {
            boolean result = false;
            if (isRegexp) {
                result = test.matches(topic);
            } else {
                result = matchWildcard(topic, test);
            }

            return result;
        }

    }
}
