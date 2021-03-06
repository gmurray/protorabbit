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
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.protorabbit.Config;
import org.protorabbit.accelerator.CacheContext;
import org.protorabbit.accelerator.ICacheable;
import org.protorabbit.accelerator.ResourceManager;
import org.protorabbit.accelerator.impl.CacheableResource;
import org.protorabbit.communicator.HandlerFactory;
import org.protorabbit.json.JSONSerializer;
import org.protorabbit.json.JSONUtil;
import org.protorabbit.json.SerializationFactory;
import org.protorabbit.model.IContext;
import org.protorabbit.model.IEngine;
import org.protorabbit.model.IProperty;
import org.protorabbit.model.ITemplate;
import org.protorabbit.model.impl.IncludeFile;
import org.protorabbit.model.impl.ResourceURI;
import org.protorabbit.profile.Episode;
import org.protorabbit.profile.Mark;
import org.protorabbit.profile.Measure;
import org.protorabbit.stats.IClientIdGenerator;
import org.protorabbit.stats.IStat;
import org.protorabbit.stats.impl.StatsItem;
import org.protorabbit.stats.impl.StatsManager;
import org.protorabbit.stringtemplate.StringTemplateEngine;
import org.protorabbit.util.IOUtil;
import java.util.Properties;

public class ProtoRabbitServlet extends HttpServlet {

    private static final long serialVersionUID = -3786248493378026969L;
    private ServletContext ctx;
    private Config jcfg;
    private boolean isDevMode = false;
    private HashMap<String, Long> lastUpdated;
    private IEngine engine;
    private JSONSerializer json = null;
    private StatsManager statsManager = null;
    private IClientIdGenerator cg = null;

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
    
    // all these values can be overridden in the default.properties file.
    // in seconds - default is 14 days
    private long maxAge = 1209600;
    private int maxTries = 250;
    // in milliseconds
    private long tryTimeout = 20;
    // default to one hour
    private long cleanupTimeout = 3600000;
    private long lastCleanup = -1;
    private boolean profile = false;
    private String buildDate = "N/A";
    private String version = "null";

    // these file types will be provided with the default expires time if run
    // through the servlet
    private String[] writeHeaders = { "gif", "jpg", "png", "css", "js"};

    public void init(ServletConfig cfg) throws ServletException {
            super.init(cfg);
            this.ctx = cfg.getServletContext();
            statsManager = (StatsManager)ctx.getAttribute(StatsManager.STATS_MANAGER);
            if ( statsManager == null) {
                throw new ServletException("You need to configure the Statis Manager as a servlet context listener in your web.xml.\n" +
                           "<listener>" +
                           " <listener-class>org.protorabbit.stats.impl.StatsManager" +
                           " </listener-class>" +
                           "</listener>");
            }
            cg = statsManager.getClientIdGenerator(ctx);
            // get default properties
            String handlerName = ctx.getInitParameter( "prt-handler-name" );
            Properties p = new Properties();
            InputStream is = getClass().getResourceAsStream("/org/protorabbit/resources/default.properties");

            try {
                p.load(is);
                version = p.getProperty( "version" );
                buildDate = p.getProperty( "buildDate" );
                cleanupTimeout = Long.parseLong((p.getProperty("cleanupTimeout")));
                maxAge = Long.parseLong((p.getProperty("maxAge")));
                maxTries = Integer.parseInt((p.getProperty("maxTries")));
                if ( handlerName == null ) {
                    handlerName = p.getProperty( "handlerName" );
                }
            } catch (Exception e1) {
                getLogger().severe("Error loading default propeteries.");
                e1.printStackTrace();
            }
            if (handlerName != null) {
                ctx.setAttribute( HandlerFactory.HANDLER_NAME, handlerName );
            }
            // set the lastCleanup to current
            lastCleanup = (new Date()).getTime();

            getLogger().info( "Protorabbit version : " + version );
            getLogger().info( "Protorabbit build date : " + buildDate );
            lastUpdated = new HashMap<String, Long>();

            if (ctx.getInitParameter("prt-dev-mode") != null) {
                isDevMode = ("true".equals(ctx.getInitParameter("prt-dev-mode").toLowerCase()));
            }
            if (ctx.getInitParameter("prt-profile") != null) {
                profile = ("true".equals(ctx.getInitParameter("prt-profile").toLowerCase()));
            }
            if (ctx.getInitParameter("prt-service-uri") != null) {
                serviceURI = ctx.getInitParameter("prt-service-uri");
            }
            if (ctx.getInitParameter("prt-expires-mappings") != null) {
                String expiresMappings = ctx.getInitParameter("prt-expires-mappings");
                if (expiresMappings.indexOf(",") != -1) {
                    writeHeaders = expiresMappings.split(",");
                } else {
                    writeHeaders = new String[1];
                    writeHeaders[0] = expiresMappings;
                }
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
            jcfg =  Config.getInstance(serviceURI, maxAge);
            jcfg.setDevMode(isDevMode);
            jcfg.setProfile(profile);
            if (engineClassName != null) {
                jcfg.setEngineClassName(engineClassName);
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

         if ((needsUpdate) && templates.length > 0) {
            jcfg.resetTemplates();
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
        int resourceType = Config.UNKNOWN;
        if (lastDot != -1) {
            String extension = id.substring(lastDot +1, id.length());
            if ("css".equals(extension)) {
                resourceType = Config.STYLE;
            } else if ("js".equals(extension)) {
                resourceType = Config.SCRIPT;
            }
            id = id.substring(0, lastDot);
        }

        String resourceId = id;
        String templateId = req.getParameter("tid");
        if (templateId != null ) {
            resourceId = templateId + "_" + resourceId;
        }

        boolean shouldGzip = false;
        ITemplate t = null;
        if (templateId != null) {
             t = jcfg.getTemplate(templateId, wc );
             wc.setTemplateId(templateId);
        }

        ResourceManager crm = jcfg.getCombinedResourceManager();
        ICacheable cr = crm.getResource(resourceId, wc);
        boolean requiresUAHandling = false;
        boolean hasUATest = false;
        if (t != null) {
            if (resourceType == Config.SCRIPT) {
                hasUATest = t.hasUserAgentScriptDependencies(wc);
                if (hasUATest) {
                    String uaTest = wc.getUAScriptTests().get(0);
                    requiresUAHandling = wc.uaTest(uaTest);
                }
            } else  if (resourceType == Config.STYLE) {
                hasUATest = t.hasUserAgentStyleDependencies(wc);
                if (hasUATest) {
                    String uaTest = wc.getUAStyleTests().get(0);
                    requiresUAHandling = wc.uaTest(uaTest);
                }
            }
        }
        // re-constitute the resource. This case will happen across server restarts
        // where a client may have a resource reference with a long cache time
        if ("protorabbit".equals(id)) {
            cr =  crm.getResource("protorabbit", wc);
            if (cr == null) {
                StringBuffer buff = IOUtil.getClasspathResource(jcfg, Config.PROTORABBIT_CLIENT);
                if (buff != null) {
                    String hash = IOUtil.generateHash(buff.toString());
                    cr = new CacheableResource("text/javascript", jcfg.getMaxAge(), hash);
                    jcfg.getCombinedResourceManager().putResource("protorabbit", cr);
                    cr.setContent(buff);
                } else {
                    getLogger().severe("Unable to find protorabbit client script");
                }
            }
        } else if ("episodes".equals(id)) {
                cr =  crm.getResource("episodes", wc);
                if (cr == null) {
                    StringBuffer buff = IOUtil.getClasspathResource(jcfg, Config.EPISODES_CLIENT);
                    if (buff != null) {
                        String hash = IOUtil.generateHash(buff.toString());
                        cr = new CacheableResource("text/javascript", jcfg.getMaxAge(), hash);
                        jcfg.getCombinedResourceManager().putResource("episodes", cr);
                        cr.setContent(buff);
                    } else {
                        getLogger().severe("Unable to find episodes client script");
                    }
                }

        } else if (cr == null && t != null && resourceId != null || requiresUAHandling ||
            (cr != null && cr.getCacheContext().isExpired())) {
            getLogger().fine("Re-constituting " + id + " from  template " + templateId);

            IProperty property = null;
            if ("styles".equals(id)) {
                List<ResourceURI> styles = t.getAllStyles(wc);
                crm.processStyles(styles, wc, out);
                cr = crm.getResource(resourceId, wc);
            } else if ("scripts".equals(id)){

                List<ResourceURI> scripts = t.getAllScripts(wc);
                crm.processScripts(scripts, wc, false, out);
                cr = crm.getResource(resourceId, wc);
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
                if (t != null) {
                    property = t.getPropertyById(id, wc);
                }
                if (property == null) {
                    getLogger().severe("Unable to find property with id " + id + ((t != null) ? " in template " + t.getId() : " with no template."));
                    return;
                }
            }
            // now before we do the work set the cache header
            if ( canGzip ) {
                if ("scripts".equals(id) &&
                     t.gzipScripts( wc) != null && t.gzipScripts( wc ) == true) {
                    shouldGzip = true;
                    resp.setHeader("Content-Type", "text/javascript");
                } else if ("styles".equals(id) && 
                        t.gzipStyles( wc ) != null && t.gzipStyles( wc ) == true) {
                    shouldGzip = true;
                    resp.setHeader("Content-Type", "text/css");
                } else if (property != null && t.gzipTemplate( wc ) != null && t.gzipTemplate( wc ) == true) {
                    shouldGzip = true;
                    resp.setHeader("Content-Type", "text/html");
                }
            }
            // gzip needs to be set before we do anything else given a call to the RequestDispatcher will
            // stop all further headers
            if (shouldGzip) {
                resp.setHeader("Content-Encoding", "gzip");
                resp.setHeader("Vary", "Accept-Encoding");
            }
            if (property != null ) {
                StringBuffer buff = null;

                IncludeFile inc = jcfg.getIncludeFileContent( property.getKey(),wc);
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
            } else if ("styles".equals(id)) {
                if (cr == null) {
                    cr = crm.getResource(resourceId, wc);
                }
            } else if ("scripts".equals(id)){
                if (cr == null) {
                    cr = crm.getResource(resourceId, wc);
                }
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
            }
        } else if (cr == null) {
            getLogger().severe("Could not find resource " + id + " with template " + templateId);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (cr != null) {
            CacheContext cc = cr.getCacheContext();

            if (cc.isExpired() || cr.getContent() == null || cr.getStatus() == ICacheable.INITIALIZED) {
                cr.refresh(wc);
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
            long timeout = 0;
            if (t != null && t.getTimeout( wc ) != null) {
                timeout = t.getTimeout( wc );
            }
            if (etag != null && t != null && timeout > 0 && !jcfg.profile()) {
                resp.setHeader("ETag", etag);
                resp.setHeader("Expires", cc.getExpires());
                resp.setHeader("Cache-Control", "public,max-age=" + cc.getMaxAge());
            }

            if (shouldGzip) {

                byte[] bytes = cr.getGZippedContent();
                cr.incrementGzipAccessCount();
                if (bytes != null) {
                    ByteArrayInputStream bis = new ByteArrayInputStream(
                            bytes);
                    IOUtil.writeBinaryResource(bis, out);
                }
            } else {
                cr.incrementAccessCount();
                out.write(cr.getContent().toString().getBytes());
            }

        }
        long now = (new Date()).getTime();
        if (now - lastCleanup > cleanupTimeout) {
            getLogger().info("Protorabbit cleaning up old Objects");
            jcfg.getCombinedResourceManager().cleanup(maxAge);
            lastCleanup = now;
        }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                        throws IOException, javax.servlet.ServletException {
        try {
            doGet(req,resp);
        } catch (java.net.SocketException jos) {
            logger.warning("Got broken pipe. Ignoring...");
        }
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

        WebContext wc = null;
        int bytesServed = 0;
        long iStartTime = System.currentTimeMillis();
        String path = req.getServletPath();
        String pathInfo = req.getPathInfo();
        String clientId = req.getRemoteAddr();
        try {

        String command = req.getParameter("command");
        if (command != null) {
            if ("ping".equals(command)) {
                resp.setHeader("pragma", "NO-CACHE");
                resp.setHeader("Cache-Control", "no-cache");
                resp.getWriter().write((new Date()).getTime() + "");
                return;
            } else if ("timeshift".equals(command)) {
                long clientTime = Long.parseLong(req.getParameter("clientTime"));
                resp.setHeader("pragma", "NO-CACHE");
                resp.setHeader("Cache-Control", "no-cache");
                long timeShift = ((new Date()).getTime() - clientTime);
                resp.getWriter().write("timeshift=" + timeShift + ";");
                return;
            } else if ("episodesync".equals(command)) {
                long startTime = Long.parseLong(req.getParameter("timestamp"));
                long transitTime = Long.parseLong(req.getParameter("transitTime"));
                Episode e = jcfg.getEpisodeManager().getEpisode(clientId, startTime);
                if (e == null) {
                    return;
                }
                e.setTransitTime(transitTime);
                Mark m = e.getMark("transit_to");
                long transitStartTime = m.getStartTime();
                long now = (new Date()).getTime();
                long duration = (now - (transitStartTime + transitTime));
                // add the page load directly following the start time  (add 1 to always make sure it is after transit time)
                e.addMark(new Mark("page_load", transitStartTime + transitTime + 1));
                Measure m1 = new Measure("transit_to", transitTime);
                // include transit time for this request and intial page load
                Measure m2 = new Measure("page_load", (duration + transitTime));
                e.addMeasure("transit_to", m1);
                e.addMeasure("page_load", m2);
                // now - duration is assumed transit time to offset call to this command
                resp.getWriter().write("var t_firstbyte=new Number(new Date());" +
                                       "window.postMessage(\"EPISODES:mark:firstbyte:\" + t_firstbyte, \"*\");");
                return;
            } else if ("stats".equals( command ) ) {

                Map<String, Object> stats = new HashMap<String, Object> ();
                stats.put("cachedResources",  jcfg.getCombinedResourceManager().getResources());
                stats.put("templates",  jcfg.getTemplates());
                stats.put("includeFiles", jcfg.getIncludeFiles());
                if (json == null) {
                    SerializationFactory factory = new SerializationFactory();
                    json = factory.getInstance();
                }
                resp.setHeader("pragma", "NO-CACHE");
                resp.setHeader("Cache-Control", "no-cache");
                Object jo = json.serialize(stats);
                resp.getWriter().write(jo.toString());
                return;
            } else if ("recordProfile".equals(command) ) {

                long startTime = Long.parseLong(req.getParameter("timestamp"));
                long timeshift = Long.parseLong(req.getParameter("timeshift"));
                long timestamp = (new Date()).getTime();
                long duration = timestamp - startTime;
                Episode e = jcfg.getEpisodeManager().getEpisode(clientId, startTime);
                if (e == null) {
                    getLogger().severe("Unable to find episode " + startTime + " to recourd data into with client " + clientId);
                    return;
                }
                e.setTimeshift(timeshift);
                // make sure to account for transit time
                Measure m = new Measure("full_request", duration - e.getTransitTime());
                e.addMeasure("full_request", m);
                String data = req.getParameter("data");
                JSONObject jo = null;
                try {
                    jo = new JSONObject(data);
                    jcfg.getEpisodeManager().updateEpisode(clientId, startTime, jo);
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
                resp.getWriter().write("ok");
                return;
            } else if ("episodes".equals(command) ) {
                if (json == null) {
                    SerializationFactory factory = new SerializationFactory();
                    json = factory.getInstance();
                }
                Object data = null;
                data = jcfg.getEpisodeManager().getEpisodes();
                resp.setHeader("pragma", "NO-CACHE");
                resp.setHeader("Cache-Control", "no-cache");
                Object jo = json.serialize(data);
                resp.getWriter().write(jo.toString());
                return;
            } else if ("version".equals(command) ) {
                resp.getWriter().write(version);
                return;
            } else if ("resetProfiles".equals(command)) {
                jcfg.getEpisodeManager().reset();
                resp.getWriter().write("profiles reset");
                return;
            } else if ("startProfiling".equals(command)) {
                jcfg.setProfile(true);
                resp.getWriter().write("profiling enabled");
                return;
            } else if ("stopProfiling".equals(command)) {
                jcfg.setProfile(false);
                resp.getWriter().write("profiling disabled");
                return;
            }
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
        wc = new WebContext(jcfg, ctx, req, resp);
        wc.setAttribute(Config.START_TIME, new Long(new Date().getTime()));
        String id = req.getParameter("resourceid");
        if (id != null) {
            processResourceRequest(id, wc, req, resp, canGzip);
            return;
        }

        if (("/" + serviceURI).equals(path)) {
            path = req.getPathInfo();
        }
        int lastSep = -1;
        if (path != null) {
            lastSep = path.lastIndexOf("/");
        }
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
            t = jcfg.getTemplate(id, wc);

            if (jcfg.profile()) {
                long timestamp = (new Date()).getTime();
                Episode e = new Episode(timestamp);
                e.setUserAgent(req.getHeader("user-agent"));
                e.setClientId(clientId);
                e.setUri(id);
                e.addMark(new Mark("full_request", timestamp));
                e.addMark(new Mark("server_render", timestamp));
                wc.setAttribute(Config.EPISODE, e);
                wc.setAttribute(Config.DEFAULT_EPISODE_PROCESS, new Boolean(true));
                jcfg.getEpisodeManager().addEpisode(e);
            }
        }
        // make sure that if a namespace is required that is is used to access the template. Also account for "" which can 
        // result from the namespace.
        boolean namespaceOk = true;
        if (t != null && t.getURINamespace( wc ) != null ) {
            if (namespace == null || (namespace != null && "".equals(namespace))|| !t.getURINamespace( wc ).startsWith(namespace)) {
                namespaceOk = false;
                getLogger().warning("request for template " + id + " without matching namespace " + t.getURINamespace( wc ));
            }
        }

        if (id == null || t == null || !namespaceOk) {
            getLogger().warning("template " + id + " requested but not found.");
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        // set the template engine
        IEngine renderEngine = null;
        if ( t.getTemplateURI(wc).getFullURI().endsWith(".st") ) {
            renderEngine = new StringTemplateEngine();
            // build up a list of the session/reqeust attributes for string tempalte
            Map<String,Object>sessionMap = new HashMap<String,Object>();
            HttpSession hs = req.getSession();
            Enumeration en = hs.getAttributeNames();
            while ( en.hasMoreElements() ) {
                String key = (String)en.nextElement();
                sessionMap.put( key , hs.getAttribute( key ) );
            }
            Map<String,Object>requestMap = new HashMap<String,Object>();
            Enumeration ren = req.getAttributeNames();
            while ( ren.hasMoreElements() ) {
                String key = (String)en.nextElement();
                requestMap.put( key , req.getAttribute( key ) );
            }
            wc.setAttribute( "session", sessionMap );
            req.getSession().setAttribute("protorabbitVersion", version );
            req.getSession().setAttribute("protorabbitBuildDate", buildDate );
            wc.setAttribute( "request", requestMap );
        } else {
            renderEngine = engine;
        }
        // buffer the output stream
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        ICacheable tr = t.getTemplateResource();
        resp.setHeader("Content-Type", "text/html");
        if (jcfg.profile()) {
            resp.setHeader("pragma", "NO-CACHE");
            resp.setHeader("Cache-Control", "no-cache");
        }

        // get the initial content or get the content if it is expired
        if ( (t.getTimeout( wc ) != null && (t.getTimeout( wc ) > 0) &&
                ((tr == null || tr.getCacheContext().isExpired() ) ||
                t.requiresRefresh(wc) ||
                jcfg.profile() ||
                t.hasUserAgentPropertyDependencies(wc) ))
            ) {
            if (canGzip && t.gzipTemplate( wc ) != null && t.gzipTemplate( wc ) == true) {
                resp.setHeader("Vary", "Accept-Encoding");
                resp.setHeader("Content-Encoding", "gzip");
            }
            // headers after this point do not get written
            renderEngine.renderTemplate(id, wc, bos);

            String content = bos.toString(jcfg.getEncoding());
            String hash = IOUtil.generateHash(content);
            ICacheable cr = new CacheableResource("text/html", t.getTimeout( wc ), hash);

            if (!jcfg.profile()) {
                resp.setHeader("ETag", cr.getContentHash());
            }
            cr.setContent(new StringBuffer(content));
            t.setTemplateResource(cr);

            if ( canGzip  && t.gzipTemplate( wc ) != null && t.gzipTemplate( wc ) == true ) {
                byte[] bytes = cr.getGZippedContent();
                cr.incrementGzipAccessCount();
                resp.setContentLength(bytes.length);
                OutputStream out = resp.getOutputStream();
                if (bytes != null) {
                    ByteArrayInputStream bis = new ByteArrayInputStream( bytes );
                    bytesServed = bytes.length;
                    IOUtil.writeBinaryResource(bis, out);
                }
            } else {
                OutputStream out = resp.getOutputStream();
                byte[] bytes = cr.getContent().toString().getBytes();
                cr.incrementAccessCount();
                resp.setContentLength(bytes.length);
                bytesServed = bytes.length;
                if (bytes != null) {
                    ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                    IOUtil.writeBinaryResource(bis, out);
                } 
            }

            // write out content / gzip or otherwise from the cache
        } else if (t.getTimeout( wc ) != null && t.getTimeout( wc ) > 0 && tr != null) {

            // if the client has the same resource as the one on the server return a 304
            // get the If-None-Match header
            String etag = tr.getContentHash();

            String ifNoneMatch = req.getHeader("If-None-Match");
            if (etag != null && ifNoneMatch != null &&
                ifNoneMatch.equals(etag)) {
                resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                if (jcfg.profile()) {
                    profile(wc);
                }
                return;
            }

            resp.setContentType(tr.getContentType());
            if (!jcfg.profile()) {
                resp.setHeader("ETag", etag);
                resp.setHeader("Expires", tr.getCacheContext().getExpires());
                resp.setHeader("Cache-Control", "public,max-age=" + tr.getCacheContext().getMaxAge());
            }

            if (canGzip &&  t.gzipTemplate( wc ) != null && t.gzipTemplate( wc ) == true) {

                OutputStream out = resp.getOutputStream();
                resp.setHeader("Content-Encoding", "gzip");
                resp.setHeader("Vary", "Accept-Encoding");

                tr.incrementGzipAccessCount();
                byte[] bytes = tr.getGZippedContent();

                if (bytes != null) {
                    resp.setContentLength(bytes.length);
                    bytesServed = bytes.length;
                    ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                    IOUtil.writeBinaryResource(bis, out);
                }
            } else {

                OutputStream out = resp.getOutputStream();
                tr.incrementAccessCount();
                byte[] bytes = tr.getContent().toString().getBytes();
                resp.setContentLength(bytes.length);
                if (bytes != null) {
                    ByteArrayInputStream bis = new ByteArrayInputStream( bytes);
                    bytesServed = bytes.length;
                    IOUtil.writeBinaryResource(bis, out);
                }
            }

        } else {
            OutputStream out = resp.getOutputStream();
          //  t.getTemplateResource().incrementAccessCount();
            renderEngine.renderTemplate(id, wc, bos);
            bytesServed = bos.size();
            out.write(bos.toByteArray());
        }
        // increment the total template accesses
        if (t != null) {
            t.incrementAccessCount();
        }
        if (jcfg.profile()) {
            profile(wc);
        }
        
        } catch (java.net.SocketException jos) {
            logger.warning("Got broken pipe. Ignoring...");
            return;
        } finally {
            if (wc != null) {
                wc.destroy();
            }
        }

        long endTime = System.currentTimeMillis();
        // add more stats stuff
        IStat stat = new StatsItem();
        stat.setTimestamp( System.currentTimeMillis() );
        stat.setPath( path );
        stat.setPathInfo( pathInfo );
        stat.setRemoteClient( cg.getClientId( req ) );
        stat.setType( StatsItem.types.VIEW );
        stat.setRequestURI( req.getRequestURI() );
        stat.setProcessTime( new Long( endTime - iStartTime) );
        stat.setContentLength( new Long(bytesServed) );
        stat.setContentType( "text/html" );
        statsManager.add( stat );

    }

    public void profile(IContext wc) {
        long startTime = ((Long)wc.getAttribute(Config.START_TIME)).longValue();
        long timestamp = (new Date()).getTime();
        long duration = timestamp - startTime;
        Episode e = (Episode)wc.getAttribute(Config.EPISODE);
        Measure m = new Measure("server_render", duration);
        e.addMeasure("server_render", m);
        e.addMark(new Mark("transit_to", timestamp));
    }
}