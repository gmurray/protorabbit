package org.protorabbit.communicator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("serial")
public class PubSubServlet extends HttpServlet {

    protected HashMap<String, MappingObject> mappings;
    protected ServletContext                 ctx;
    protected PubSub                         ps;
    protected PollManager pm = null;
    public void init(ServletConfig cfg) {
        try {
            super.init(cfg);
            this.ctx = cfg.getServletContext();
            this.ps = PubSub.getInstance();
            this.mappings = new HashMap<String, MappingObject>();
            pm = new PollManager(this.ctx);
            // test

        } catch (ServletException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // TODO Auto-generated method stub
        super.doDelete(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // default is to return the messages
        JSONArray ma = ps.getMessages();
        resp.getOutputStream().print(ma.toString());
        ps.clearMessages();
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // TODO Auto-generated method stub
        super.doHead(req, resp);
    }

    @Override
    protected void doOptions(HttpServletRequest arg0, HttpServletResponse arg1)
            throws ServletException, IOException {
        // TODO Auto-generated method stub
        super.doOptions(arg0, arg1);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String topic = req.getParameter("topic");

        if ("/initialize".equals(topic)) {
            resp.getOutputStream().println("{ \"status\" : \"ok\"}");
        }

        String message = req.getParameter("message");

        JSONObject jo = null;
        if (message != null) {
            try {
                jo = new JSONObject(message);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if ("message" != null) {
            System.out.println("message is " + message);
        }

        if ("topic" != null) {

            // process for subscribers
            ps.processRequest(topic, jo);

            if (mappings.get(topic) != null) {
                MappingObject mo = mappings.get(topic);
                System.out.println("We have a topic match. Object is "
                        + mo.getName());

                Object target = null;

                switch (mo.getScope()) {
                case MappingObject.APPLICATION: {
                    target = ctx.getAttribute(mo.getName());
                    break;
                }
                }

                if (target != null) {
                    System.out.println("Syncing the objects.");
                    if (jo != null) {

                        Iterator<String> it = jo.keys();
                        while (it.hasNext()) {
                            String key = it.next();
                            System.out.println("cheking key " + key);
                            // jump out if the key length is too short
                            if (key.length() <= 1)
                                continue;

                            String value = null;
                            try {
                                value = jo.getString(key);
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }

                            Method m = null;
                            String mName = "set"
                                    + key.substring(0, 1).toUpperCase()
                                    + key.substring(1);
                            System.out.println("Looking for " + mName
                                    + " on Object " + mo.getName());
                            try {
                                m = target.getClass().getMethod(mName,
                                        String.class);
                                if (m != null) {
                                    System.out.println("*** Found " + mName
                                            + " on Object " + mo.getName());
                                    Object[] args = { value };
                                    m.invoke(target, args);
                                }
                            } catch (SecurityException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (NoSuchMethodException e) {
                                // TODO Auto-generated catch block
                                System.out.print("Method " + mName
                                        + " not found");
                            } catch (IllegalArgumentException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (IllegalAccessException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (InvocationTargetException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }

                        }
                    }
                }
            }
        }
        resp.getOutputStream().println(
                "{ \"status\" : \"sucess\", \"topic\" : \"" + topic + "\"}");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // TODO Auto-generated method stub
        super.doPut(req, resp);
    }

    @Override
    protected void doTrace(HttpServletRequest arg0, HttpServletResponse arg1)
            throws ServletException, IOException {
        // TODO Auto-generated method stub
        super.doTrace(arg0, arg1);
    }

    @Override
    protected long getLastModified(HttpServletRequest req) {
        // TODO Auto-generated method stub
        return super.getLastModified(req);
    }

}