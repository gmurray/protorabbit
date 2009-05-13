/*
 * Protorabbit
 *
 * Copyright (c) 2009 Greg Murray (protorabbit.org)
 * 
 * Licensed under the MIT License:
 * 
 *  http://www.opensource.org/licenses/mit-license.php
 *
 */

package org.protorabbit.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.protorabbit.Config;
import org.protorabbit.model.impl.BaseContext;
import org.protorabbit.util.IOUtil;

public class WebContext extends BaseContext {

    private Config cfg;
    private ServletContext sctx;
    private HttpServletRequest req;

    private HttpServletResponse resp;
    private String contextRoot = "";

    public WebContext(Config cfg) {
        this.cfg = cfg;
    }

    public WebContext(Config cfg, ServletContext sctx, HttpServletRequest req, HttpServletResponse resp) {
        this.cfg = cfg;
        this.sctx = sctx;
        this.req = req;
        this.resp = resp;
        contextRoot = req.getContextPath();
    }

    public void setServletContext(ServletContext sctx) {
        this.sctx = sctx;
    }

    public Config getConfig() {
        return cfg;
    }

    public boolean resourceExists(String name) {
        URL url = null;
        try {
            url = sctx.getResource(name);
        } catch (MalformedURLException e) {
            // eat the exception
        }
        return (url != null);
    }

    public boolean isUpdated(String name, long lastUpdate) {
        URL url = null;
        try {
            url = sctx.getResource(name);
            if (url != null) {
                URLConnection uc = url.openConnection();
                long lastMod = uc.getLastModified();
                return (lastMod > lastUpdate);
            } else {
                Config.getLogger().warning("Error locating resource : " + name);
                return true;
            }
        } catch (MalformedURLException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public long getLastUpdated(String name) {
        URL url = null;
        try {
            url = sctx.getResource(name);
            if (url != null) {
                URLConnection uc = url.openConnection();
                long lastMod = uc.getLastModified();
                return lastMod;
            } else {
                Config.getLogger().warning("Error locating resource : " + name);
                return -1;
            }
        } catch (MalformedURLException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public StringBuffer getResource(String baseDir, String name) throws IOException {

        String resourceName = null;

        if (name.startsWith("/")) {
            resourceName = name;
        } else {
            resourceName = baseDir + name;
        }
        if (name.endsWith(".jsp")) {

                try {

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    BufferedServletResponse br = new BufferedServletResponse(resp, bos);
                    RequestDispatcher rd = req.getRequestDispatcher(resourceName);
                    if (rd != null) {
                        rd.include(req, br);
                    }
                    br.flushBuffer();
                    StringBuffer buff = new StringBuffer(bos.toString(cfg.getEncoding()));
                    bos.close();

                    return buff;

                } catch (Throwable t) {
                    t.printStackTrace();
                }

        } else {

            InputStream is = sctx.getResourceAsStream(resourceName);
            if (is != null) {
                StringBuffer contents = IOUtil.loadStringFromInputStream(is, cfg.getEncoding());
                   return new StringBuffer(contents);
            } else {
                // don't throw out the resource name to end user
                Config.getLogger().log(Level.SEVERE, "Error loading " + resourceName);
                throw new IOException("Error loading resource. Please notify the administrator that there was an issue.");
            }
        }
        return null;
    }

    /*
     * 
     * Parses a expression like request.user.name
     * Which translates to : http servlet request.getAttribute("user").getName();
     * 
     * The scopes are: 
     * 
     * request - HttpServlet Request
     * session - HttpSession
     * context - ServletContext
     * none - protorabbit context (similar to request) 
     *
     */
    public Object parseExpression(String expression) {

        String scope = null;
        String[] path = {expression};
        if (expression.indexOf(".") != -1) {
            path = expression.split("\\.");
        }
        Object target = null;
        if (path.length > 1) {
            scope = path[0];
            if ("request".equals(scope)) {
                target = getRequest().getAttribute(path[1]);
            } else if ("session".equals(scope) && getRequest().getSession() != null) {
                target = getRequest().getSession().getAttribute(path[1]);
            } else if ("context".equals(scope) && getRequest().getSession() != null) {
                target = getRequest().getSession().getServletContext().getAttribute(path[1]);
            } else {
                target = getAttribute(expression);
            }
        } else {
            target = getAttribute(expression);
        }

        int start = 1;
        if (scope != null) {
            start += 1;
        }
        // if there is anything below the scope and object
        for (int i=start; target != null && i < path.length; i++) {
            target = getObject(target, path[i]);
        }

        return target;
    }

    public Object getObject(Object pojo, String target) {
        String getTarget = "get" + target.substring(0,1).toUpperCase() + target.substring(1);
        Object[] args = {};
        Method[] methods = pojo.getClass().getDeclaredMethods();

        for (int i=0; i < methods.length;i++) {
            try {
                Method m = methods[i];
                if (Modifier.isPublic(m.getModifiers()) &&
                   ( m.getName().equals(getTarget) || 
                     m.getName().equals(target))) {
                    // change the case of the property from camelCase
                    Object value =  m.invoke(pojo, args);
                    return value;
                }
            } catch (IllegalArgumentException e) {
                Config.getLogger().warning("Non Fatal Error looking up property : " + target + " on object " + pojo + " " + e);
            } catch (IllegalAccessException e) {
               Config.getLogger().warning("Non Fatal Error looking up property : " + target + " on object " + pojo + " " + e);
            } catch (InvocationTargetException e) {
                Config.getLogger().warning("Non Fatal Error looking up property : " + target + " on object " + pojo + " " + e);
            }
        }
        return null;
    }

    public String getContextRoot() {
        return contextRoot;
    }

    /*
     * Test whether an expression matches the user agent
     */
    public boolean test(String test) {
        boolean notTest = false;
        boolean matches = false;
        Object lvalue = null;
        Object rvalue = null;

        int equalStart = test.indexOf("==");
        int notEqualStart = test.indexOf("!=");

        // we are an equal test
        if (equalStart != -1) {
            lvalue = findValue(test.substring(0, equalStart));
            rvalue = findValue(test.substring(equalStart+2));
        } else if (notEqualStart != -1){
            notTest = true;
            lvalue = findValue(test.substring(0, notEqualStart));
            rvalue = findValue(test.substring(notEqualStart+2));
        }

        if (lvalue != null && rvalue != null &&
            String.class.isAssignableFrom(lvalue.getClass())) {
            if (String.class.isAssignableFrom(rvalue.getClass())) {
                matches = ((String)lvalue).equals((String)rvalue);
            }
        } else if (lvalue != null && rvalue != null &&
                Boolean.class.isAssignableFrom(lvalue.getClass())) {
            if (Boolean.class.isAssignableFrom(rvalue.getClass())) {
                matches = ((Boolean)lvalue).equals((Boolean)rvalue);
            }
        } else if (lvalue != null && rvalue != null &&
                Number.class.isAssignableFrom(lvalue.getClass())) {
            if (Number.class.isAssignableFrom(rvalue.getClass())) {
                matches = ((Number)lvalue).byteValue() == ((Number)rvalue).byteValue();
            }
        } else {
            matches = (lvalue == rvalue);
        }
        if (notTest) {
            return (!matches);
        } else {
            return matches;
        }
    }

    /*
     * Test whether an expression matches the user agent
     */
    public boolean uaTest(String test) {
        boolean matches = false;
        String userAgent = req.getHeader("User-Agent");
        if (userAgent != null) {
            Pattern p = Pattern.compile(test);
            Matcher m = p.matcher(userAgent);
            return m.find();
        }
        return matches;
    }

    public HttpServletRequest getRequest() {
        return req;
    }

    public HttpServletResponse getResponse() {
        return resp;
    }
}
