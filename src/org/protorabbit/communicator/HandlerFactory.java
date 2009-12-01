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
import org.protorabbit.util.ClassUtil;

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

        getLogger().info("The request path is " + path );
        getLogger().info("pathInfo is : " + pathInfo);

        String handler = null;
        String handlerMethod = null;
        String handlerNameSpace = null;

        int namespace = path.lastIndexOf("/");
        getLogger().info("namespace=" + namespace);
        if (namespace != -1) {
            handlerNameSpace = path.substring( 0,namespace );
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
        getLogger().info("handler=" + handler);
        getLogger().info("handlerMethod=" + handlerMethod);
        getLogger().info("handlerNamespace=" + handlerNameSpace);

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
                Class<?> klass = this.getClass().getClassLoader().loadClass(s + "." + klassName);
                try {
                    Object target = klass.newInstance();
                    thandler = (Handler)target;
                    if (handlerMethod != null) {
                        // check for the Namespace
                        if (klass.isAnnotationPresent(Namespace.class)) {
                            Namespace n = (Namespace) klass.getAnnotation(Namespace.class);
                            if (n != null && handlerNameSpace != null) {
                                    if (handlerNameSpace.equals(n.value())) {
                                        getLogger().info("Namespace match. Continuing");
                                    } else {
                                        getLogger().info("Namespace " + n.value() + " required.");
                                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                                        return;
                                    }
                           } else {
                               getLogger().info("Namespace " + n.value() + " required.");
                               response.sendError(HttpServletResponse.SC_FORBIDDEN);
                               return;
                           }
                        }
                        try {
                            Object [] args = {};
                            Class<?> [] cargs = {};
                            Method m = thandler.getClass().getMethod(handlerMethod, cargs);
                            
                            if (m.getReturnType() == String.class) {
                                try {
                                    result = (String)m.invoke(target, args);
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
            getLogger().info("Could not find a handler with name " + klassName + " in any of the search packages.");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        getLogger().info("##We have the handler " + thandler);
        // map parameters to the setters
        mapParameters(thandler,request);
        processHandler(thandler,result, request,response);

    }
    
    @SuppressWarnings("unchecked")
	protected void mapParameters(Handler h,
                           HttpServletRequest request){
        Enumeration<String> params = request.getParameterNames();
        while(params.hasMoreElements()) {
            String param = params.nextElement();
            getLogger().info("Processing param " + param);
            // get a list of possible methods that are setters for this param
            List<Method> methods = ClassUtil.getMethods(h, param, true);
            // populate the values
            ClassUtil.mapParam(methods, param, request.getParameter(param), h);
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
        // only for testing
//        response.setHeader("Content-Type", "text/html");
        response.setHeader("Content-Type", "application/json");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("pragma", "NO-CACHE");
        // now that we have the json object print out the string
        Object responseObject = jsonSerializer.serialize(jr);
        response.getWriter().write(responseObject.toString());
    }

}
