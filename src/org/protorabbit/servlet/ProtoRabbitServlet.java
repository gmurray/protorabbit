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
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import org.protorabbit.accelerator.CacheContext;
import org.protorabbit.accelerator.ICacheable;
import org.protorabbit.accelerator.ResourceManager;
import org.protorabbit.accelerator.impl.CacheableResource;
import org.protorabbit.json.JSONSerializer;
import org.protorabbit.json.JSONUtil;
import org.protorabbit.json.SerializationFactory;
import org.protorabbit.model.IEngine;
import org.protorabbit.model.IProperty;
import org.protorabbit.model.ITemplate;
import org.protorabbit.model.impl.IncludeFile;
import org.protorabbit.model.impl.ResourceURI;
import org.protorabbit.util.IOUtil;

public class ProtoRabbitServlet extends HttpServlet {

    private static final long serialVersionUID = -3786248493378026969L;
    private ServletContext ctx;
    private Config jcfg;
    private boolean isDevMode = false;
    private HashMap<String, Long> lastUpdated;
    private IEngine engine;
    private JSONSerializer json = null;

    private static Logger logger = null;

    static final Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("org.protrabbit");
        }
        return logger;
    }

    private String[] templates = null;

    // defaults
    private String engineClassName = null;
    private String defaultTemplateURI = "/WEB-INF/templates.json";
    private String serviceURI = "prt";

    private long maxAge = 1225000;
    private int maxTries = 300;
    // in milliseconds
    private long tryTimeout = 20;
    // default to one hour
    private long cleanupTimeout = 3600000;
    private long lastCleanup = -1;

    private String version = "0.7-dev-c";

    private String[] writeHeaders = { "gif", "jpg", "png"};

    public void init(ServletConfig cfg) throws ServletException {

            super.init(cfg);

            // set the lastCleanup to current
            lastCleanup = (new Date()).getTime();

            getLogger().info("Protorabbit version : " + version);
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
                   getLogger().warning("Non-fatal: Error processing configuration : prt-service-uri must be a long.");
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
                    getLogger().log(Level.SEVERE, "Fatal Error: Unable to instanciate engine class " + engineClassName, e);
                    throw new ServletException("Fatal Error: Unable to reading in configuration class " + engineClassName, e);
                }
             engine = jcfg.getEngine();

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
                getLogger().severe("Error reading configuration. Could not find " + templateName);
            }
        }

         if ((jcfg == null || needsUpdate) && templates.length > 0) {
            jcfg = new Config(serviceURI, maxAge);
            jcfg.setDevMode(isDevMode);
            if (engineClassName != null) {
                jcfg.setEngineClassName(engineClassName);
            }
            engine = jcfg.getEngine();
            for (int i = 0; i < templates.length; i++) {
                JSONObject base = null;
                InputStream is = this.ctx.getResourceAsStream(templates[i]);
                if (is != null) {
                    base = JSONUtil.loadFromInputStream(is);
                } else {
                    getLogger().log(Level.SEVERE, "Error  loading " + templates[i]);
                    throw new IOException("Error  loading " + templates[i] + ": Please verify the file exists.");
                }
                if (base == null) {
                    getLogger().log(Level.SEVERE, "Error  loading " + templates[i]);
                    throw new IOException("Error  loading" + templates[i] + ": Please verify the file is correctly formatted.");
                }
                String baseURI = getTemplateDefDir(templates[i]);
                try {
                    JSONArray templatesArray = base.getJSONArray("templates");
                    if (templatesArray != null) {
                        jcfg.registerTemplates(templatesArray, baseURI);
                    }
                    updateLastModified(templates[i]);
                    getLogger().info("Registered " + templates[i]);
                } catch (JSONException e1) {
                    e1.printStackTrace();
                } catch (Exception ex){
                    getLogger().log(Level.SEVERE, "Error  loading" + templates[i], ex);
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
            getLogger().warning("Error checking for last modified on:  " + uri);
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
        String resourceId = id;
        String templateId = req.getParameter("tid");
        if (templateId != null ) {
            resourceId = templateId + "_" + resourceId;
        }
        ResourceManager crm = jcfg.getCombinedResourceManager();
        ICacheable cr = crm.getResource(resourceId);

        // re-constitute the resource. This case will happen across server restarts
        // where a client may have a resource reference with a long cache time
        if (cr == null && templateId != null && resourceId != null) {
            getLogger().fine("Re-constituting " + id + " from  template " + templateId);
            ITemplate t = jcfg.getTemplate(templateId);
            if ("styles".equals(id)) {
                List<ResourceURI> styles = t.getAllStyles(wc);
                crm.processStyles(styles, wc, out);
                cr = crm.getResource(resourceId);
            } else if ("scripts".equals(id)){
                List<ResourceURI> scripts = t.getAllScripts(wc);
                crm.processStyles(scripts, wc, out);
                cr = crm.getResource(resourceId);
            } else if ("messages".equals(id)) {
                if (json == null) {
                    SerializationFactory factory = new SerializationFactory();
                    json = factory.getInstance();
                }
                List<IProperty> deferredProperties = new ArrayList<IProperty>();
                t.getDeferProperties(deferredProperties, wc);
                JSONObject jo = (JSONObject)json.serialize(deferredProperties);
                String content = jo.toString();
                cr = new CacheableResource("application/json", jcfg.getResourceTimeout(), resourceId);
                cr.setContent( new StringBuffer(content) );
                crm.putResource(resourceId, cr);
                // assume this is a request for a deferred resource that hasn't been created
            } else {
                IProperty property = t.getPropertyById(id, wc);
                StringBuffer buff = null;
                IncludeFile inc = jcfg.getIncludeFileContent(templateId, property.getKey(),wc);
                if (inc != null) {
                    buff = inc.getContent();
                    String hash  = IOUtil.generateHash(buff.toString());
                    if (property.getId() != null) {
                        resourceId = property.getId();
                    } else {
                        resourceId = hash;
                    }
                    cr = new CacheableResource("text/html", inc.getTimeout(), hash);
                    cr.setContent( buff );
                    crm.putResource(templateId + "_" + resourceId , cr);
                }
            }
        } else if ("protorabbit".equals(id)) {
            StringBuffer buff = IOUtil.getClasspathResource(jcfg, Config.PROTORABBIT_CLIENT);
            if (buff != null) {
                String hash = IOUtil.generateHash(buff.toString());
                cr = new CacheableResource("text/javascript", jcfg.getMaxAge(), hash);
                jcfg.getCombinedResourceManager().putResource("protorabbit", cr);
                cr.setContent(buff);
            } else {
                getLogger().severe("Unable to find protorabbit client script");
            }
        } else if (cr == null) {
            getLogger().severe("could not find resource " + id + " with template " + templateId);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
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
                cr.incrementAccessCount();
                if (bytes != null) {
                    ByteArrayInputStream bis = new ByteArrayInputStream(
                            bytes);
                    IOUtil.writeBinaryResource(bis, out);
                }
            } else {
                resp.setHeader("Content-Type", cr.getContentType());
                out.write(cr.getContent().toString().getBytes());
            }

        }
        long now = (new Date()).getTime();
        if (now - lastCleanup > cleanupTimeout) {
            getLogger().info("Cleaning up old Objects");
            jcfg.getCombinedResourceManager().cleanup(maxAge);
            lastCleanup = now;
        }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                        throws IOException, javax.servlet.ServletException {
        doGet(req,resp);
    }

    private void writeHeaders(HttpServletRequest req, HttpServletResponse resp, String path) throws ServletException, IOException {

        String expires = IOUtil.getExpires(jcfg.getResourceTimeout());
        resp.setHeader("Expires", expires);
        resp.setHeader("Cache-Control", "public,max-age=" + expires);
        req.getRequestDispatcher(path).forward(req, resp);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getServletPath();
        String pathInfo = req.getPathInfo();
        if ( "stats".equals(req.getParameter("command") ) ) {

            Map<String, Object> stats = new HashMap<String, Object> ();
            stats.put("cachedResources",  jcfg.getCombinedResourceManager().getResources());
            stats.put("templates",  jcfg.getTemplates());
            stats.put("includeFiles", jcfg.getIncludeFiles());
            if (json == null) {
                SerializationFactory factory = new SerializationFactory();
                json = factory.getInstance();
            }
            Object jo = json.serialize(stats);
            resp.getWriter().write(jo.toString());
            return;
        } else if ("version".equals(req.getParameter("command") ) ) {
            resp.getWriter().write(version);
            return;
        } else if (pathInfo != null) {
            for (String t : writeHeaders) {
               if (pathInfo.endsWith(t)) {
                   writeHeaders(req, resp, pathInfo);
                   return;
               }
            }
        }

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

        if (("/" + serviceURI).equals(path)) {
            path = req.getPathInfo();
        }
        int lastSep = path.lastIndexOf("/");
        String namespace = null;
        if (lastSep != -1 && lastSep < path.length() - 1) {
            int nextDot = path.indexOf(".", lastSep + 1);
            int lastSlash = path.lastIndexOf("/");
            if (nextDot != -1) {
                id = path.substring(lastSep + 1, nextDot);
            } else {
                if ( lastSlash != -1 && lastSlash < path.length()) {
                    id = path.substring(lastSlash + 1);
                }
            }
            if ( lastSlash != -1 && lastSlash < path.length()) {
                namespace = path.substring(0, lastSlash);
            }
        }
        ITemplate t = null;

        if (id != null) {
            t = jcfg.getTemplate(id);
        }
        boolean namespaceOk = true;
        if (t != null && t.getURINamespace() != null ) {
            if (namespace == null || !t.getURINamespace().startsWith(namespace)) {
                namespaceOk = false;
                getLogger().warning("request for template " + id + " without matching namespace " + t.getURINamespace());
            }
        }
        if (id == null || t == null || !namespaceOk) {
            getLogger().warning("template " + id + " requested but not found.");
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // buffer the output stream
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        ICacheable tr = t.getTemplateResource();

        // get the initial content or get the content if it is expired
        if (t.getTimeout() != null && 
                ((t.getTimeout() > 0 && (tr == null ||
                tr.getCacheContext().isExpired() )) ||
                t.requiresRefresh(wc) ||
                t.hasUserAgentDependencies(wc) )) {

            if (canGzip && t.gzipTemplate() != null && t.gzipTemplate()) {
                resp.setHeader("Vary", "Accept-Encoding");
                resp.setHeader("Content-Encoding", "gzip");
            }

            resp.setHeader("Expires", IOUtil.getExpires(t.getTimeout()));
            resp.setHeader("Cache-Control", "public,max-age=" + IOUtil.getMaxAge(t.getTimeout()));

            resp.setHeader("Content-Type", "text/html");

            // headers after this point do not get written
            engine.renderTemplate(id, wc, bos);

            String content = bos.toString(jcfg.getEncoding());
            String hash = IOUtil.generateHash(content);
            ICacheable cr = new CacheableResource("text/html", t.getTimeout(), hash);
            resp.setHeader("ETag", cr.getContentHash());

            cr.setContent(new StringBuffer(content));
            t.setTemplateResource(cr);

            if (canGzip  &&  t.gzipTemplate() != null && t.gzipTemplate()) {
                byte[] bytes = cr.getGZippedContent();
                cr.incrementAccessCount();
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
                cr.incrementAccessCount();
                resp.setContentLength(bytes.length);
                if (bytes != null) {
                    ByteArrayInputStream bis = new ByteArrayInputStream(
                            bytes);
                    IOUtil.writeBinaryResource(bis, out);
                } 
            }

            // write out content / gzip or otherwise from the cache
        } else if (t.getTimeout() != null && t.getTimeout() > 0 && tr != null) {

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
            engine.renderTemplate(id, wc, bos);
            out.write(bos.toByteArray());
        }

    }
}