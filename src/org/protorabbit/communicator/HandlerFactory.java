package org.protorabbit.communicator;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.protorabbit.json.JSONSerializer;
import org.protorabbit.json.SerializationFactory;

public class HandlerFactory {

    protected List<String> searchPackages = new ArrayList<String>();
    private JSONSerializer jsonSerializer;
    ServletContext ctx = null;

    public HandlerFactory(ServletContext ctx) {
        this.ctx = ctx;
    }

    private static Logger logger = null;

    static final Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("org.protrabbit");
        }
        return logger;
    }

    public void addSearchPackage(String p) {
        searchPackages.add(p);
    }

    public void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // find the suitable action
        String path = request.getServletPath();
        String pathInfo = request.getPathInfo();

        System.out.println("request path is " + path );
        System.out.println("pathInfo is : " + pathInfo);

        String handler = null;
        String handlerMethod = null;
        String handlerNameSpace = null;

        int namespace = path.lastIndexOf("/");
        System.out.println("namespace=" + namespace);
        if (namespace != -1) {
            handlerNameSpace = path.substring(namespace,namespace +1);
            namespace +=1;
        } else {
            namespace = 0;
        }

        int startHandlerMethod = path.indexOf("!");
        int endHandler = path.lastIndexOf(".");
        
        if (startHandlerMethod != -1 && endHandler != -1) {
            handlerMethod = path.substring(startHandlerMethod +1, endHandler);
            handler = path.substring(namespace, startHandlerMethod);
        } else if (endHandler != -1){
            handler = path.substring(namespace, endHandler);
        }
        System.out.println("handler=" + handler);
        System.out.println("handlerMethod=" + handlerMethod);
        System.out.println("handlerNamespace=" + handlerNameSpace);

        Handler thandler = null;
        String result = null;

        // send a 404
        if (handler == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String klassName = handler.substring(0,1).toUpperCase() + 
                           handler.substring(1,handler.length()) + "Handler";
        for (String s : searchPackages) {
            try {
                Class klass = this.getClass().getClassLoader().loadClass(s + "." + klassName);
                try {
                    Object target = klass.newInstance();
                    thandler = (Handler)target;
                    if (handlerMethod != null) {

                        try {
                            Method m = thandler.getClass().getMethod(handlerMethod, null);
                            
                            if (m.getReturnType() == String.class) {
                                try {
                                    result = (String)m.invoke(target, null);
                                } catch (SecurityException e) {
                                    getLogger().log(Level.WARNING, "SecurityException invoking Handler " + handlerMethod);
                                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                    return;
                                } catch (InvocationTargetException e) {
                                    getLogger().log(Level.WARNING, "InvocationTargetException invoking Handler " + handlerMethod);
                                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                    return;
                                }
                            } else {
                                getLogger().log(Level.WARNING, "Handler " + handlerMethod + " must return a String");
                                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                                return;
                            }

                        }catch (NoSuchMethodException e) {
                            getLogger().log(Level.WARNING, "Handler " + handlerMethod + " not found.");
                            response.sendError(HttpServletResponse.SC_NOT_FOUND);
                            return;
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                        // invoke default handler
                    } else {
                        result = thandler.doExecute();
                    }
                } catch (InstantiationException e) {
                    getLogger().log(Level.WARNING, "InstantiationException creating Handler " + handlerMethod);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                } catch (IllegalAccessException e) {
                    getLogger().log(Level.WARNING, "IllegalAccessException creating Handler " + handlerMethod);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                } catch (ClassCastException cce) {
                    getLogger().log(Level.WARNING, "ClassCastException creating Handler " + klassName);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }
            } catch (ClassNotFoundException e) {
                getLogger().log(Level.WARNING, "Handler " + klassName + " not found.");
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }
        if (thandler == null) {
            System.out.println("Could not find a handler with name " + klassName + " in any of the search packages.");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        System.out.println("##We have the handler " + thandler);
        // map parameters to the setters
        mapParameters(thandler,request);
        processHandler(thandler,result, request,response);

    }
    
    protected void mapParameters(Handler h,
                           HttpServletRequest request){
        Enumeration params = request.getParameterNames();
        while(params.hasMoreElements()) {
            String param = (String)params.nextElement();
            System.out.println("Processing param " + param);
        }
    }

    protected void processHandler(Handler h, String result,
                                  HttpServletRequest request,
                                  HttpServletResponse response) throws IOException {

        if (this.jsonSerializer == null) {
            jsonSerializer = new SerializationFactory().getInstance();
        }

        JSONResponse jr = new JSONResponse();
        if (h.isPoller()) {
            PollManager pm = (PollManager)ctx.getAttribute(PollManager.POLL_MANAGER);
            jr.setPollInterval(pm.getPollInterval(request));
        }

        if (h.getErrors() != null) {
            jr.setResult("error");
            jr.setErrors(h.getErrors());
        } else {
            jr.setResult(result);
        }

        // get the model
        jr.setData( h.getModel() );

        response.setHeader("Content-Type", "text/html");
//        response.setHeader("Content-Type", "application/json");
        // now that we have the json object print out the string
        Object responseObject = jsonSerializer.serialize(jr);
        response.getWriter().write(responseObject.toString());
    }

}
