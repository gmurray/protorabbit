package org.protorabbit.communicator;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
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
import org.protorabbit.stats.IClientIdGenerator;
import org.protorabbit.stats.IStat;
import org.protorabbit.stats.impl.PollManager;
import org.protorabbit.stats.impl.StatsItem;
import org.protorabbit.stats.impl.StatsManager;
import org.protorabbit.util.ClassUtil;

public class HandlerFactory {

    public static String HANDLER_NAME = "org.protorabbit.HANDLER_NAME";

    protected List<String> searchPackages = new ArrayList<String>();
    private JSONSerializer jsonSerializer;
    private String handlerName = "Handler";
    private StatsManager statsManager = null;
    private IClientIdGenerator cg = null;

    public HandlerFactory(ServletContext ctx) {
        statsManager = (StatsManager)ctx.getAttribute(StatsManager.STATS_MANAGER);
        cg = statsManager.getClientIdGenerator( ctx );
        String cHName = (String)ctx.getAttribute( HANDLER_NAME );
        if ( cHName != null ) {
            handlerName = cHName;
        }
    }

    private static Logger logger = null;

    static final Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("org.protrabbit");
        }
        return logger;
    }

    /*
     * Printout a stacktrace until the desired class method if stopAt is provided
     * For example java.lang.Method.invoke
     * 
     * This allows for more readable relevant stack trace messages.
     * 
     */
    public static String getStackTraceAsString( Throwable t, String stopAt ) {
        String out = "";
        for ( StackTraceElement te : t.getCause().getStackTrace() ) {

            if ( stopAt != null ) {
                String methodName = te.getClassName() + "." + te.getMethodName();
                if ( methodName.equals( stopAt ) ) {
                    break;
                }
            }
            out += " " + te.toString() + "\n";
        }
        return out;
      }

    public void addSearchPackage(String p) {
        searchPackages.add(p);
    }

    public void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // find the suitable action
        String path = request.getServletPath();

        String handler = null;
        String handlerMethod = null;
        String handlerNameSpace = null;

        int namespace = path.lastIndexOf("/");

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

        Handler thandler = null;
        String result = null;

        // send a 404
        if (handler == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        // find the handler class
        String klassName = handler.substring(0,1).toUpperCase() + 
                           handler.substring(1,handler.length()) + handlerName;
        Class<?> klass = null;
        for ( String s : searchPackages) {
            try {
                klass = this.getClass().getClassLoader().loadClass(s + "." + klassName);
                // break if we find it
                break;
            } catch (ClassNotFoundException e) {
                // do nothing 
            }
        }
        // if we have the class invoke it
        if ( klass == null) {
            getLogger().log(Level.WARNING, "Handler " + klassName + " not found in search packages.");
            response.sendError( HttpServletResponse.SC_NOT_FOUND );
            return;
        } else {
            try {
                    Object target = klass.newInstance();
                    thandler = (Handler)target;
                    // prepare the handler by setting the request / response
                    thandler.setRequest(request);
                    thandler.setResponse(response);

                    // map parameters to the setters
                    mapParameters(thandler,request);

                    if (handlerMethod != null) {
                        // check for the Namespace
                        if (klass.isAnnotationPresent(Namespace.class)) {
                            Namespace n = (Namespace) klass.getAnnotation(Namespace.class);
                            if (n != null && handlerNameSpace != null) {
                                    if (handlerNameSpace.equals(n.value())) {
                                        // a match so we keep going
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
                                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, getStackTraceAsString( e, klass.getName() + "." + handlerMethod ) );
                                    return;
                                } catch (InvocationTargetException e) {
                                    // TODO : consider unwinding a few of the top layers to get the core exception
                                    getLogger().log( Level.SEVERE, "Error invoking Handler " + handlerMethod, e );
                                    thandler.addActionError( "Error invoking " + m.getName() + " : " + getStackTraceAsString( e, klass.getName() + "." + handlerMethod ) );
                                }
                            } else {
                                getLogger().log(Level.WARNING, "Handler " + handlerMethod + " must return a String");
                                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                                return;
                            }

                        } catch (NoSuchMethodException e) {
                            getLogger().log(Level.WARNING, "Handler " + handlerMethod + " not found.");
                            response.sendError(HttpServletResponse.SC_NOT_FOUND);
                            return;
                        } catch (IllegalArgumentException e) {
                            getLogger().log( Level.WARNING, e.getLocalizedMessage() );
                        }
                        // invoke default handler
                    } else {
                        result = thandler.doExecute();
                    }
                } catch (InstantiationException e) {
                    getLogger().log(Level.WARNING, "InstantiationException creating Handler " + handlerMethod);
                    response.getOutputStream().write(e.getLocalizedMessage().getBytes());
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                } catch (IllegalAccessException e) {
                    getLogger().log(Level.WARNING, "IllegalAccessException creating Handler " + handlerMethod);
                    response.getOutputStream().write(e.getLocalizedMessage().getBytes());
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                } catch (ClassCastException cce) {
                    getLogger().log(Level.WARNING, "ClassCastException creating Handler " + klassName);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                    // all other errors end in a error message
                } catch (Throwable e) {
                    if (thandler != null ) {
                        thandler.addActionError( e.getLocalizedMessage() );
                    }
                    getLogger().log(Level.SEVERE, "Error processing function: ", e);
                }
        }
        int bytesServed = 0;
        if (thandler == null) {
            getLogger().info("Could not find a handler with name " + klassName + " in any of the search packages.");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            // do the JSON post processing
            bytesServed = processHandler(thandler,result, request,response);
        } catch (IOException iox ) {
            thandler.getErrors().add( iox.getLocalizedMessage() );
        } finally {

            // record stats
            IStat stat = new StatsItem();
            if ( thandler.isPoller() ) {
                stat.setIsPoller( true );
            }
            stat.setTimestamp( System.currentTimeMillis() );
            stat.setPath( path );
            stat.setPathInfo( request.getPathInfo() );
            stat.setRemoteClient( cg.getClientId(request) );
            stat.setType( StatsItem.types.JSON );
            stat.setRequestURI( request.getRequestURI() );
            stat.setContentLength( new Long(bytesServed) );
            if ( result != BaseJSONHandler.BINARY ) {
                stat.setContentType( "application/json" );
            } else {
                stat.setContentType( response.getContentType() );
            }
            if ( (thandler.getErrors() != null && thandler.getErrors().size() > 0) ) {
                stat.setHasErrors( true );
                stat.setErrors( thandler.getErrors() );
            }
            long endTime = System.currentTimeMillis();
            Long st = (Long)request.getAttribute( CommunicatorServlet.START_REQUEST_TIMESTAMP );
            long iStartTime = 0;
            if ( st != null) {
                iStartTime = st.longValue();
                stat.setProcessTime( new Long( endTime - iStartTime) );
            }
            statsManager.add( stat );
        }

    }

    @SuppressWarnings("unchecked")
    protected void mapParameters(Handler h,
                           HttpServletRequest request){
        Enumeration<String> params = request.getParameterNames();
        while(params.hasMoreElements()) {
            String param = params.nextElement();
            // get a list of possible methods that are setters for this param
            List<Method> methods = ClassUtil.getMethods(h, param, true);
            // populate the values
            ClassUtil.mapParam(methods, param, request.getParameter(param), h);
        }
    }

    protected int processHandler(Handler h, String result,
                                  HttpServletRequest request,
                                  HttpServletResponse response) throws IOException {

        if (this.jsonSerializer == null) {
            jsonSerializer = new SerializationFactory().getInstance();
        }

        JSONResponse jr = new JSONResponse();
        if (h.isPoller()) {
            PollManager pm = PollManager.getInstance();
            jr.setPollInterval(pm.getPollInterval(request));
        }

        if (h.getErrors() != null) {
            jr.setResult("error");
            jr.setErrors(h.getErrors());
        } else {
            jr.setResult(result);
        }
        int bytesServed = 0;
        // get the model
        jr.setData( h.getModel() );
        if ( result != BaseJSONHandler.BINARY ) {
            response.setHeader("Content-Type", "application/json");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("pragma", "NO-CACHE");
            // now that we have the json object print out the string
            Object responseObject = jsonSerializer.serialize(jr);
            response.getWriter().write(responseObject.toString());
            bytesServed = responseObject.toString().getBytes().length;
        } else {
            Integer bs = (Integer)request.getAttribute( "org.protorabbit.BYTES_SERVED");
            if ( bs != null ) {
                bytesServed = bs.intValue();
            }
        }
        return bytesServed;
    }

}
