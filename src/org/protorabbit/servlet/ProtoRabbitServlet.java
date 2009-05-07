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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.logging.Level;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.protorabbit.Config;
import org.protorabbit.IEngine;
import org.protorabbit.accelerator.CacheContext;
import org.protorabbit.accelerator.ICacheable;
import org.protorabbit.accelerator.impl.CacheableResource;
import org.protorabbit.json.JSONUtil;
import org.protorabbit.model.ITemplate;
import org.protorabbit.util.IOUtil;

@SuppressWarnings("serial")
public class ProtoRabbitServlet extends HttpServlet {

    private ServletContext ctx;
    private Config jcfg;
    private boolean isDevMode = false;
    private HashMap<String, Long> lastUpdated;
    private IEngine engine;

    private String[] templates = null;

    // defaults
    private String engineClassName = "org.protorabbit.impl.DefaultEngine";
    private String defaultTemplateURI = "/WEB-INF/templates.json";
    private String serviceURI = "prt";

    private long maxAge = 1225000;
    private int maxTries = 300;
    private long tryTimeout = 20;

    private long cleanupTimeout = 60000;
    private long lastCleanup = -1;

    private String version = "0.5-dev";

    @SuppressWarnings("unchecked")
    public void init(ServletConfig cfg) throws ServletException {

            super.init(cfg);

            // set the lastCleanup to current
            lastCleanup = (new Date()).getTime();

            Config.getLogger().info("Protorabbit version : " + version);
            lastUpdated = new HashMap<String, Long>();
            this.ctx = cfg.getServletContext();
            if (ctx.getInitParameter("prt-dev-mode") != null) {
                isDevMode = ("true".equals(ctx.getInitParameter("prt-dev-mode").toLowerCase()));
            }
            if (ctx.getInitParameter("prt-service-uri") != null) {
                serviceURI = ctx.getInitParameter("prt-service-uri");
            }
            if (ctx.getInitParameter("prt-engine-class") != null) {
                engineClassName = ctx.getInitParameter("prt-engine-class");
            }
            if (ctx.getInitParameter("prt-max-timeout") != null) {
                String maxTimeoutString =  ctx.getInitParameter("prt-max-timeout");
                try {
                    maxAge = (new Long(maxTimeoutString)).longValue();
                } catch (Exception e) {
                   Config.getLogger().warning("Non-fatal: Error processing configuration : prt-service-uri must be a long.");
                }
            }
            if (ctx.getInitParameter("prt-templates") != null) {
                String tString = ctx.getInitParameter("prt-templates");
                // clean up the templates string
                tString = tString.trim();
                tString = tString.replace(" ", "");
                templates = tString.split(",");
            } else {
                templates = new String[] { defaultTemplateURI };
            }
            try {
                updateConfig();
                } catch (IOException e) {
                    Config.getLogger().log(Level.SEVERE, "Fatal Error: Unable to instanciate engine class " + engineClassName, e);
                    throw new ServletException("Fatal Error: Unable to reading in configuration class " + engineClassName, e);
                }
            // initialize the engine
            Class<IEngine> clazz = null;
            try {
                clazz = (Class<IEngine>) Class.forName(engineClassName);
                engine = clazz.newInstance();
            } catch (ClassNotFoundException cnfe) {
                Config.getLogger().severe("Fatal Error: Unable to find engine class " + engineClassName);
                throw new ServletException("Fatal Error: Unable to find engine class " + engineClassName);
            } catch (InstantiationException e) {
                Config.getLogger().log(Level.SEVERE, "Fatal Error: Instantiation exception for engine class " + engineClassName, e);
                throw new ServletException("Fatal Error: Instantiation exception for engine class " + engineClassName, e);
            } catch (IllegalAccessException e) {
                Config.getLogger().log(Level.SEVERE, "Fatal Error: Unable to access engine class " + engineClassName, e);
                throw new ServletException("Fatal Error: Unable to access engine class " + engineClassName, e);
            }

    }

    void updateConfig() throws IOException {
        boolean needsUpdate = false;
        String templateName = null;
        for (int i = 0; i < templates.length; i++) {
            try {
                templateName = templates[i];
                URL turl = ctx.getResource(templateName);
                URLConnection uc = turl.openConnection();
                long lastMod = uc.getLastModified();
                Long lu = lastUpdated.get(templates[i]);
                long lastTime = 0;
                if (lu != null) {
                    lastTime = lu.longValue();
                }
                if (lastMod > lastTime) {
                    needsUpdate = true;
                    break;
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (NullPointerException npe) {
                Config.getLogger().severe("Error reading configuration. Could not find " + templateName);
            }
        }

         if ((jcfg == null || needsUpdate) && templates.length > 0) {
            jcfg = new Config(serviceURI, maxAge);
            jcfg.setDevMode(isDevMode);
            for (int i = 0; i < templates.length; i++) {
                JSONObject base = null;
                InputStream is = this.ctx.getResourceAsStream(templates[i]);
                if (is != null) {
                    base = JSONUtil.loadFromInputStream(is);
                } else {
                    Config.getLogger().log(Level.SEVERE, "Error  loading " + templates[i]);
                    throw new IOException("Error  loading " + templates[i] + ": Please verify the file exists.");
                }
                
                if (base == null) {
                    Config.getLogger().log(Level.SEVERE, "Error  loading " + templates[i]);
                    throw new IOException("Error  loading" + templates[i] + ": Please verify the file is correctly formatted.");
                }
                String baseURI = getTemplateDefDir(templates[i]);
                try {
                    JSONArray templatesArray = base.getJSONArray("templates");
                    if (templatesArray != null) {
                        jcfg.registerTemplates(templatesArray, baseURI);
                    }
                    updateLastModified(templates[i]);
                    Config.getLogger().info("Registered " + templates[i]);
                } catch (JSONException e1) {
                    e1.printStackTrace();
                } catch (Exception ex){
                    Config.getLogger().log(Level.SEVERE, "Error  loading" + templates[i], ex);
                }
            }
        }
    }

    public String getTemplateDefDir(String location) {

        int last = location.lastIndexOf("/");
        if (last != -1) {
            return location.substring(0, last + 1);
        }
        return null;
    }

    void updateLastModified(String uri) throws IOException {
        URL turl = ctx.getResource(uri);
        if (turl != null) {
            URLConnection uc = turl.openConnection();
            long lastMod = uc.getLastModified();
            lastUpdated.put(uri, lastMod);
        } else {
            Config.getLogger().warning("Error checking for last modified on:  " + uri);
        }
    }

    /*
     * Process a request for an external resource
     */
    void processResourceRequest(String id, WebContext wc,
                                HttpServletRequest req,
                                HttpServletResponse resp,
                                boolean canGzip) throws IOException {

        OutputStream out = resp.getOutputStream();
        int lastDot = id.lastIndexOf(".");
        if (lastDot != -1) {
            id = id.substring(0, lastDot);
        }

        ICacheable cr = jcfg.getCombinedResourceManager().getResource(id);

        if (cr == null) {
            Config.getLogger().severe("could not find resource " + id);
        }

        if (cr != null) {
            CacheContext cc = cr.getCacheContext();
            if (cc.isExpired() || cr.getContent() == null) {
                if (jcfg.getGzip() && canGzip && cr.gzipResources()) {
                    resp.setHeader("Content-Encoding", "gzip");
                    cr.refresh(wc);
                }
            }
            // wait for the resource to load
            int tries = 0;
            while ((cr.getStatus() != 200) && tries < maxTries) {
                try {
                    Thread.sleep(tryTimeout);
                } catch (InterruptedException e) {
                }
                tries += 1;
            }
            if (cr.getStatus() != 200) {
                resp.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT);
                return;
            }
            if (cr.getContentType() != null) {
                resp.setContentType(cr.getContentType());
            }

            String etag = cr.getContentHash();
            // get the If-None-Match header
            String ifNoneMatch = req.getHeader("If-None-Match");
            if (etag != null &&
                ifNoneMatch != null && 
                ifNoneMatch.equals(etag)) {
                resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            }
            if (etag != null) {
                resp.setHeader("ETag", etag);
            }
            resp.setHeader("Expires", cc.getExpires());
            resp.setHeader("Cache-Control", "public,max-age=" + cc.getMaxAge());

            if (jcfg.getGzip() && canGzip && cr.gzipResources()) {
                resp.setHeader("Content-Encoding", "gzip");
                byte[] bytes = cr.getGZippedContent();
                if (bytes != null) {
                    ByteArrayInputStream bis = new ByteArrayInputStream(
                            bytes);
                    IOUtil.writeBinaryResource(bis, out);
                }
            } else {
                resp.setHeader("Content-Type", cr.getContentType());
                out.write(cr.getContent().toString().getBytes());
            }

        } else {
            Config.getLogger().warning("resource " + id +
                    " requested but not found.");
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        long now = (new Date()).getTime();
        if (now - lastCleanup > cleanupTimeout) {
            Config.getLogger().info("Cleaning up old Objects");
            jcfg.getCombinedResourceManager().cleanup(maxAge);
        }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                        throws IOException, javax.servlet.ServletException {
        doGet(req,resp);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // check for updates to the templates.json file
        if (isDevMode) {
            updateConfig();
        }

        boolean canGzip = false;
        // check if client supports gzip
        Enumeration<String> hnum = req.getHeaders("Accept-Encoding");
        while (hnum.hasMoreElements()) {
            String acceptType = hnum.nextElement();
            if (acceptType != null && acceptType.indexOf("gzip") != -1) {
                canGzip = true;
                break;
            }
        }
        WebContext wc = new WebContext(jcfg, ctx, req, resp);
        String id = req.getParameter("resourceid");

        if (id != null) {
            processResourceRequest(id, wc, req, resp, canGzip);
            return;
        }

        String servletPath = req.getServletPath();
        int lastSep = servletPath.lastIndexOf("/");

        if (lastSep != -1 && lastSep < servletPath.length() - 1) {
            int nextDot = servletPath.indexOf(".", lastSep + 1);
            if (nextDot != -1) {
                id = servletPath.substring(lastSep + 1, nextDot);
            }
        }

        ITemplate t = null;

        if (id != null) {
            t = jcfg.getTemplate(id);
        }
        if (id == null || t == null) {
            Config.getLogger().warning("template " + id + " requested but not found.");
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // buffer the output stream
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        ICacheable tr = t.getTemplateResource();

        // get the initial content or get the content if it is expired
        if ((t.getTimeout() > 0 && (tr == null ||
                tr.getCacheContext().isExpired() )) ||
                t.requiresRefresh(wc) ||
                t.hasUserAgentDependencies(wc)) {

            if (canGzip && t.gzipTemplate() != null && t.gzipTemplate()) {
                resp.setHeader("Vary", "Accept-Encoding");
                resp.setHeader("Content-Encoding", "gzip");
            }

            resp.setHeader("Expires", IOUtil.getExpires(t.getTimeout()));
            resp.setHeader("Cache-Control", "public,max-age=" + IOUtil.getMaxAge(t.getTimeout()));

            resp.setHeader("Content-Type", "text/html");

            // headers after this point do not get written
            engine.renderTemplate(id, jcfg, bos, wc);

            String content = bos.toString("UTF8");
            String hash = IOUtil.generateHash(content);
            ICacheable cr = new CacheableResource("text/html", t.getTimeout(), hash);
            resp.setHeader("ETag", cr.getContentHash());

            cr.setContent(new StringBuffer(content));
            t.setTemplateResource(cr);

            if (canGzip  &&  t.gzipTemplate() != null && t.gzipTemplate()) {
                byte[] bytes = cr.getGZippedContent();
                resp.setContentLength(bytes.length);
                OutputStream out = resp.getOutputStream();
                if (bytes != null) {
                    ByteArrayInputStream bis = new ByteArrayInputStream(
                            bytes);
                    IOUtil.writeBinaryResource(bis, out);
                }
            } else {
                OutputStream out = resp.getOutputStream();
                byte[] bytes = cr.getContent().toString().getBytes();
                resp.setContentLength(bytes.length);
                if (bytes != null) {
                    ByteArrayInputStream bis = new ByteArrayInputStream(
                            bytes);
                    IOUtil.writeBinaryResource(bis, out);
                } 
            }

            // write out content / gzip or otherwise from the cache
        } else if (t.getTimeout() > 0 && tr != null) {

            // if the client has the same resource as the one on the server return a 304
            // get the If-None-Match header
            String etag = tr.getContentHash();
            
            String ifNoneMatch = req.getHeader("If-None-Match");
            if (etag != null && ifNoneMatch != null &&
                ifNoneMatch.equals(etag)) {
                resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }

            resp.setContentType(tr.getContentType());

            resp.setHeader("ETag", etag);
            resp.setHeader("Expires", tr.getCacheContext().getExpires());
            resp.setHeader("Cache-Control", "public,max-age=" + tr.getCacheContext().getMaxAge());

            if (canGzip) {

                OutputStream out = resp.getOutputStream();
                resp.setHeader("Content-Encoding", "gzip");
                resp.setHeader("Vary", "Accept-Encoding");
                byte[] bytes = tr.getGZippedContent();

                if (bytes != null) {
                    resp.setContentLength(bytes.length);
                    ByteArrayInputStream bis = new ByteArrayInputStream(
                           bytes);

                    IOUtil.writeBinaryResource(bis, out);
                }
            } else {

                OutputStream out = resp.getOutputStream();
                byte[] bytes =tr.getContent().toString().getBytes();
                resp.setContentLength(bytes.length);
                if (bytes != null) {
                    ByteArrayInputStream bis = new ByteArrayInputStream(
                            bytes);
                    IOUtil.writeBinaryResource(bis, out);
                }
            }

        } else {
            OutputStream out = resp.getOutputStream();
            engine.renderTemplate(id, jcfg, bos, wc);
            out.write(bos.toByteArray());
        }

    }
}