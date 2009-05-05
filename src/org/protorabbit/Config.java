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

package org.protorabbit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.protorabbit.accelerator.CombinedResourceManager;
import org.protorabbit.model.ICommand;
import org.protorabbit.model.IContext;
import org.protorabbit.model.IProperty;
import org.protorabbit.model.ITemplate;
import org.protorabbit.model.impl.IncludeCommand;
import org.protorabbit.model.impl.IncludeFile;
import org.protorabbit.model.impl.PropertyImpl;
import org.protorabbit.model.impl.ResourceURI;
import org.protorabbit.model.impl.TemplateImpl;

public class Config {

    private static Logger logger = null;

    public static Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("org.protrabbit");
        }
        return logger;
    }

    private Map<String, Object> globalAttributes = null;
    
    public static long DEFAULT_TIMEOUT = 60 * 1000 * 15;

    private long resourceTimeout = DEFAULT_TIMEOUT;

    String encoding = "UTF-8";

    String defaultMediaType = "screen, projection";
    String commandBase = "";

    Map<String, ITemplate> tmap = null;
    Map<String, IncludeFile> includeFiles = null;
    Map<String, String> commandMap = null;

    boolean gzip = true;
    boolean defaultCombineResources = false;
    boolean devMode = false;

    CombinedResourceManager crm = null;

    long combinedResourceTimeout = 60 * 1000 + 60 * 24;

    // in seconds 
    private long maxAge = 1225000;
    private String resourceService =  "prt";

    public Config(String serviceURI, long maxAge ) {
        init();
        this.maxAge = maxAge;
        crm = new CombinedResourceManager(this,
                serviceURI,
                getMaxAge());
    }

    public Config() {
        init();
        crm = new CombinedResourceManager(this,
                                          resourceService,
                                         getMaxAge());
    }

    void init() {
        globalAttributes = new HashMap<String, Object>();
        commandMap = new HashMap<String, String>();

        // include mappings for the default commands
        commandMap.put("insert", "org.protorabbit.model.impl.InsertCommand");
        commandMap.put("include", "org.protorabbit.model.impl.IncludeCommand");
        commandMap.put("includeResources", "org.protorabbit.model.impl.IncludeResourcesCommand");

        tmap = new HashMap<String, ITemplate>();
        includeFiles = new HashMap<String, IncludeFile>();
    }

    public Object getGlobalAttribute(String key) {
        return globalAttributes.get(key);
    }

    public void setGlobaAttribute(String key, Object value) {
        globalAttributes.put(key, value);
    }

    public void clearGlobalAttributes() {
        globalAttributes.clear();
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public boolean getDevMode() {
        return devMode;
    }

    public boolean getGzip() {
        return gzip;
    }

    public void setCommandBase(String commandBase) {
        this.commandBase = commandBase;
    }

    public ICommand getCommand(String name) {
        String className = commandMap.get(name);

        Class<?> clazz = null;

        // look for custom commands
        if (className == null) {
            try {
                clazz = Class.forName(commandBase + name + "Command");
            } catch (ClassNotFoundException cnfe) {
                getLogger().log(Level.SEVERE, "Error locating class impementation for command " + name + ".");

                return null;
            }
        } else {
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException cnfe) {
                getLogger().log(Level.SEVERE, "Could not find class " + className);
                return null;
            }
        }

        try {

            Object o = clazz.newInstance();
            if (o instanceof ICommand) {
                ICommand ic = (ICommand)o;
                return ic;
            } else {
                getLogger().log(Level.SEVERE, "Error creating instance of " + className +
                        ". The command needs to implement org.protorabbit.model.Command");
            }

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean hasTemplate(String id, IContext ctx) {
        ITemplate t = getTemplate(id);
        return (t != null);
    }

    public CombinedResourceManager getCombinedResourceManager() {
        return crm;
    }

    /*
     * Used when you know the resource id of the include file (the uri to the resource)
     * and you want to add the file.
     * 
     */
    public void setIncludeFile(String rid, IncludeFile inc) {
        includeFiles.put(rid, inc);
    }

    /*
     * Used when you know the resource id of the include file (the uri to the resource)
     * 
     */
    public IncludeFile getIncludeFile(String rid) {
        return includeFiles.get(rid);
    }

    public IncludeFile getIncludeFileContent(String tid, String id, IContext ctx) {
        try {
            ITemplate template = getTemplate(tid);
            if (template != null && template.getJSON() != null) {

                IProperty prop = template.getProperty(id,ctx);
                if (prop == null) {
                    getLogger().log(Level.SEVERE, "Unable to find Include file for " + id + " in template " + tid);
                    return null;
                }
                if (prop.getUATest() != null) {
                    if (ctx.uaTest(prop.getUATest()) == false) {
                        return null;
                    }
                }
                String includeFile = prop.getValue();

                String tBase = "";
                if (!includeFile.startsWith("/")) {
                    tBase = prop.getBaseURI();
                }

                String uri = tBase + includeFile;
                IncludeFile inc = null;
                if (includeFiles.containsKey(uri)) {
                    inc =  includeFiles.get(uri);
                }
                if (inc == null || (inc != null && inc.isStale(ctx))) {

                    StringBuffer buff = ctx.getResource(tBase, includeFile);
                    if (inc == null) {
                        inc = new IncludeFile(uri, buff);
                        inc.setTimeout(prop.getTimeout());
                        inc.setDefer(prop.getDefer());
                        inc.setDeferContent(prop.getDeferContent());
                        includeFiles.put(uri, inc);
                    } else {
                        inc.setContent(buff);
                    }

                    return inc;
                } else {
                    return inc;
                }
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error getting include file content for tempalte " +
                            tid + " resource " + id + ".", e);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public void registerTemplates(JSONArray templates, String baseURI) {

        for (int i=0; i < templates.length(); i++) {
            JSONObject t;
            try {
                t = templates.getJSONObject(i);
                String id = t.getString("id");

                   ITemplate temp = new TemplateImpl(id, baseURI, t, this);

                   long templateTimeout = resourceTimeout;

                   if (t.has("timeout")) {
                       templateTimeout = t.getLong("timeout");
                       temp.setTimeout(templateTimeout);
                   }

                   boolean tgzip = gzip;

                   if (t.has("gzip")) {
                       tgzip = t.getBoolean("gzip");
                       temp.setGzipStyles(tgzip);
                       temp.setGzipScripts(tgzip);
                       temp.setGzipTemplate(tgzip);
                   }

                   boolean combineResources = defaultCombineResources;

                   // template overrides default combineResources
                   if (t.has("combineResources")) {
                       combineResources = t.getBoolean("combineResources");
                       temp.setCombineScripts(combineResources);
                       temp.setCombineStyles(combineResources);
                   }

                   temp.setTimeout(templateTimeout);

                if (t.has("template")) {
                    String turi = t.getString("template");
                    ResourceURI templateURI = new ResourceURI(turi, baseURI, ResourceURI.TEMPLATE);
                    temp.setTemplateURI(templateURI);
                }

                if (t.has("extends")) {
                    List<String> ancestors = null;
                    String base = t.getString("extends");
                    if (base.length() > 0) {
                        String[] parentIds = null;
                        if (base.indexOf(",") != -1) {
                            parentIds = base.split(",");
                        } else {
                            parentIds = new String[1];
                            parentIds[0] = base;
                        }
                        ancestors = new ArrayList<String>();

                        for (int j = 0; j < parentIds.length; j++) {
                            ancestors.add(parentIds[j].trim());
                        }
                    }

                    temp.setAncestors(ancestors);
                }

                if (t.has("scripts")) {

                    JSONObject bsjo = t.getJSONObject("scripts");
                    
                    processURIResources(ResourceURI.SCRIPT,
                                        bsjo,
                                        temp,
                                        baseURI);
                }

                if (t.has("styles")) {
                    JSONObject bsjo = t.getJSONObject("styles");

                    processURIResources(ResourceURI.LINK,
                            bsjo,
                            temp,
                            baseURI);
                }

                if (t.has("properties")) {

                    Map<String,IProperty> properties = null;
                    JSONObject po = t.getJSONObject("properties");
                    properties = new  HashMap<String,IProperty>();

                    Iterator<String> jit =  po.keys();
                    while(jit.hasNext()) {

                        String name = jit.next();
                        JSONObject so = po.getJSONObject(name);
                        int type = IProperty.STRING;
                        String value = so.getString("value");

                        if (so.has("type")) {

                            String typeString = so.getString("type");
                            
                            if ("string".equals(typeString.toLowerCase())) {
                                type = IProperty.STRING;
                            } else if ("include".equals(typeString.toLowerCase())) {
                                type = IProperty.INCLUDE;
                            }
                        }

                        IProperty pi= new PropertyImpl(name, value, type, baseURI, id);
                        long timeout = templateTimeout;
                        if (so.has("timeout")) {
                            timeout = so.getLong("timeout");
                        }

                        if (so.has("uaTest")) {
                            pi.setUATest(so.getString("uaTest"));
                        }
                        if (so.has("defer")) {
                            pi.setDefer(so.getBoolean("defer"));
                        }
                        if (so.has("deferContent")) {
                            pi.setDeferContent(new StringBuffer(so.getString("deferContent")));
                        }
                        pi.setTimeout(timeout);
                        properties.put(name, pi);
                    }
                    temp.setProperties(properties);
                }

                // force combineResources if under the /WEB-INF
                if (baseURI.startsWith("/WEB-INF") && combineResources != true) {
                    getLogger().warning("Template " + temp.getId() + " is loated in a private directory " + baseURI + " without " +
                                        "combineResouces being set. It is recommended that you enable combineResouces.");
                }
               tmap.put(id, temp);

               getLogger().info("Added template definition : " + id);

            } catch (JSONException e) {
                getLogger().log(Level.SEVERE, "Error parsing configuration.", e);
            }
        }
    }

    private void processURIResources(int type,
                                JSONObject bsjo,
                                ITemplate temp,
                                String baseURI) throws JSONException {

            List<ResourceURI> refs = null;
            refs = new ArrayList<ResourceURI>();

            if (bsjo.has("libs")) {

                JSONArray ja = bsjo.getJSONArray("libs");

                for (int j=0; j < ja.length(); j++) {

                    JSONObject so = ja.getJSONObject(j);
                    String url = so.getString("url");
                    if (url.startsWith("/") || url.startsWith("http")) {
                        baseURI = "";
                    }
                    ResourceURI ri = new ResourceURI(url , baseURI, type);
                    if (so.has("id")) {
                        ri.setId(so.getString("id"));
                    }
                    if (so.has("uaTest")) {
                        ri.setUATest(so.getString("uaTest"));
                    }
                    if (so.has("defer")) {
                        ri.setDefer(so.getBoolean("defer"));
                    }
                    refs.add(ri);
                }
            }
            Boolean combine = null;
            if (bsjo.has("combineResources")) {
                combine = bsjo.getBoolean("combineResources");
            } 
            Boolean lgzip = null;
            if (bsjo.has("gzip")) {
                lgzip = bsjo.getBoolean("gzip");
            }

            if (type == ResourceURI.SCRIPT) {
                temp.setCombineScripts(combine);
                temp.setGzipScripts(lgzip);
                temp.setScripts(refs);
            } else if (type == ResourceURI.LINK) {
                temp.setGzipStyles(lgzip);
                temp.setStyles(refs);
                temp.setCombineStyles(combine);
            }
    }

    public ITemplate getTemplate(String id) {
        if (tmap.containsKey(id)) {
            return tmap.get(id);
        }
        return null;
    }

    public String generateScriptReferences(ITemplate template, IContext ctx) {
        List<ResourceURI> scripts = template.getAllScripts(ctx);
        return generateReferences(template,ctx,scripts, ResourceURI.SCRIPT);
    }

    public String generateStyleReferences(ITemplate template, IContext ctx) {
        List<ResourceURI> styles = template.getAllStyles(ctx);
        return generateReferences(template, ctx, styles, ResourceURI.LINK);
    }

    @SuppressWarnings("unchecked")
    public String generateReferences(ITemplate template, IContext ctx, List<ResourceURI> resources, int type) {
        String buff = "";

            if (resources != null) {
                List<String> deferredScripts = (List<String>)ctx.getAttribute(IncludeCommand.DEFERRED_SCRIPTS);
                Iterator<ResourceURI> it = resources.iterator();
                while (it.hasNext()) {

                    ResourceURI ri = it.next();
                    if (ri.isWritten()) continue;
                    String resource = ri.getUri();
                    String baseURI =  ctx.getContextRoot();

                    if (!ri.isExternal()){
                        // map to root
                        if (resource.startsWith("/")) {
                            baseURI = ctx.getContextRoot();
                        } else {
                           baseURI +=  ri.getBaseURI();
                        }
                    } else {
                        baseURI = "";
                    }
                    if (type == ResourceURI.SCRIPT) {
                        if (ri.isDefer()) {
                            if (deferredScripts == null) {
                                deferredScripts = new ArrayList<String>();
                            }
                            String fragement = "<script>protorabbit.addDeferredScript('" +
                                     baseURI + resource + "');</script>";
                            deferredScripts.add(fragement);
                            ri.setWritten(true);
                            ctx.setAttribute(IncludeCommand.DEFERRED_SCRIPTS, deferredScripts);
                        } else {
                            buff += "<script type=\"text/javascript\" src=\"" + baseURI + resource + "\"></script>\n";
                        }
                    } else if (type == ResourceURI.LINK) {
                        String mediaType = ri.getMediaType();
                        if (mediaType == null){
                            mediaType = defaultMediaType;
                        }
                        if (ri.isDefer()) {
                            if (deferredScripts == null) {
                                deferredScripts = new ArrayList<String>();
                            }
                            String fragement = "<script>protorabbit.addDeferredStyle('" +
                                                baseURI + resource  + "', '" + mediaType + "');</script>";
                            deferredScripts.add(fragement);
                            ri.setWritten(true);
                            ctx.setAttribute(IncludeCommand.DEFERRED_SCRIPTS, deferredScripts);

                        } else {
                            buff += "<link rel=\"stylesheet\" type=\"text/css\"  href=\"" + baseURI + resource + "\" media=\"" + mediaType + "\" />\n";
                        }
                    }
                }
            }
            return buff;
    }

    public String getTemplateURI(JSONObject template) {
        if (template.has("template")) {
            try {
                return template.getString("template");
            } catch (JSONException e) {
                getLogger().log(Level.SEVERE, "Error locating template.", e);
            }
        }
        return null;
    }

    public String getIncludeFileName(JSONObject template, String tid) {
        if (template.has("properties")) {
            try {
                JSONObject properties =  template.getJSONObject("properties");
                if (properties.has(tid)) {
                    JSONObject to = properties.getJSONObject(tid);
                    return to.getString("value");
                }
            } catch (JSONException e) {
                getLogger().log(Level.SEVERE, "Error parsing include file name.", e);
            }
        }
        return null;
    }

    public IProperty getContent(String tid, String id, IContext ctx) {
        ITemplate template = getTemplate(tid);
        if (template != null) {
            
            IProperty p = template.getProperty(id, ctx);
            if (p != null) {
                return p;
            }

        }
        return null;
    }

    public String getResourceReferences(String tid, String pid, IContext ctx) {
        ITemplate template = getTemplate(tid);
        String rtype = pid.toLowerCase();
        if (template != null && "scripts".equals(rtype)) {
            return generateScriptReferences(template, ctx);
        } else if (template != null && "styles".equals(rtype)) {
            return generateStyleReferences(template, ctx);
        }
        return null;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public long getResourceTimeout() {
        return resourceTimeout;
    }

    public void setResourceTimeout(long resourceTimeout) {
        this.resourceTimeout = resourceTimeout;
    }

    public String mediaType() {
        return defaultMediaType;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

    public String getResourceService() {
        return resourceService;
    }

    public void setResourceService(String resourceService) {
        this.resourceService = resourceService;
    }

}
