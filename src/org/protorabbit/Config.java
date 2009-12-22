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

import java.io.InputStream;
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

import org.protorabbit.accelerator.IHttpClient;
import org.protorabbit.accelerator.ResourceManager;
import org.protorabbit.model.ICommand;
import org.protorabbit.model.IContext;
import org.protorabbit.model.IEngine;
import org.protorabbit.model.IProperty;
import org.protorabbit.model.ITemplate;
import org.protorabbit.model.impl.ExtendedTemplate;
import org.protorabbit.model.impl.IncludeFile;
import org.protorabbit.model.impl.Property;
import org.protorabbit.model.impl.ResourceURI;
import org.protorabbit.model.impl.Template;
import org.protorabbit.model.impl.TemplateOverride;
import org.protorabbit.profile.EpisodeManager;
import org.protorabbit.util.IOUtil;

public class Config {

   public static final String PROTORABBIT_CLIENT = "resources/protorabbit.js";
   public static final String EPISODES_CLIENT = "resources/episodes.js";
   public static final String EPISODE_POSTER = "resources/episodes-viewer.js";
   public static final String EPISODE = "org.protorabbit.profile.episode";
   public static final String START_TIME = "org.protorabbit.START_TIME";
   public static final String DEFAULT_EPISODE_PROCESS = "org.protorabbit.profile.episode.DEFAULT_PROCESS";;

   public static final int UNKNOWN = 0;
   public static final int SCRIPT = 1;
   public static final int STYLE = 2;

   public static long DEFAULT_TIMEOUT = 60 * 1000 * 15;
   boolean gzip = true;
   boolean defaultCombineResources = false;
   boolean devMode = false;
   private String encoding = "UTF-8";
   private long resourceTimeout = DEFAULT_TIMEOUT;
   // in seconds - default is 14 days
   private long maxAge = 1209600;
   private String resourceService = "prt";
   String defaultMediaType = "screen, projection";
   String commandBase = "";
   private String engineClassName = "org.protorabbit.model.impl.DefaultEngine";
   private String httpClientClassName = "org.protorabbit.accelerator.impl.HttpClient";
   private static Logger logger = null;
   private EpisodeManager episodeManager = null;
   private long created = 0;
   private static int counter = 0;


   static Logger getLogger() {
       if (logger == null) {
           logger = Logger.getLogger("org.protrabbit");
       }
       return logger;
   }

   private static IEngine engine = null;

   private Map<String, Object> globalAttributes = null;
   Map<String, ITemplate> tmap = null;
   Map<String, IncludeFile> includeFiles = null;
   Map<String, String> commandMap = null;
   private boolean profile = false;
   private static Config config = null;

   ResourceManager crm = null;

   public void resetTemplates() {
       tmap = new HashMap<String, ITemplate>();
   }

   public static Config getInstance(String serviceURI, long maxAge ) {
       if (config == null) {
           config = new Config( serviceURI, maxAge );
       }
       return config;
   }
   
   public static Config getInstance() {
       if (config == null) {
           config = new Config();
       }
       return config;
   }

   private Config(String serviceURI, long maxAge ) {
       init();
       this.maxAge = maxAge;
       crm = new ResourceManager(this,
               serviceURI,
               getMaxAge());
   }

   private Config() {
       init();
       crm = new ResourceManager(this,
                                 resourceService,
                                 getMaxAge());
   }

   public boolean profile() {
       return profile;
   }
   
   public EpisodeManager getEpisodeManager() {
       if (episodeManager == null) {
           episodeManager = new EpisodeManager();
       }
       return episodeManager;
   }

   public long getCreateTime() {
       return created;
   }

   void init() {
       this.created = System.currentTimeMillis();
       globalAttributes = new HashMap<String, Object>();
       commandMap = new HashMap<String, String>();

       // include mappings for the default commands
       commandMap.put("insert", "org.protorabbit.model.impl.InsertCommand");
       commandMap.put("include", "org.protorabbit.model.impl.IncludeCommand");
       commandMap.put("includeResources", "org.protorabbit.model.impl.IncludeResourcesCommand");

       tmap = new HashMap<String, ITemplate>();
       includeFiles = new HashMap<String, IncludeFile>();
   }
   
   public Map<String, IncludeFile> getIncludeFiles() {
       return includeFiles;
   }
   public Map<String, ITemplate> getTemplates() {
       return tmap;
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
       ITemplate t = getTemplate(id, ctx);
       return (t != null);
   }

   public ResourceManager getCombinedResourceManager() {
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

   public IncludeFile getIncludeFileContent( String id, IContext ctx) {
       ITemplate template = null;
       String tid = null;
       try {
           template = ctx.getTemplate();

           if (template != null && template.getJSON() != null) {
               tid = template.getId();
               IProperty prop = template.getProperty(id,ctx);
               if (prop == null) {
                   getLogger().log(Level.SEVERE, "Unable to find Include file for " + id + " in template " + template.getId() );
                   return null;
               }
               if (prop.getUATest() != null) {
                   if (ctx.uaTest(prop.getUATest()) == false) {
                       // track the test
                       ctx.addUAScriptTest(prop.getUATest());
                       return null;
                   }
               }
               if (prop.getTest() != null) {
                   if (ctx.test(prop.getTest()) == false) {
                       return null;
                   }
               }
               String includeFile = prop.getValue();
               IncludeFile inc = null;
               String tBase = "";
               String uri = includeFile;
               if (!includeFile.startsWith("/") && !includeFile.startsWith("http")) {
                   tBase = prop.getBaseURI();
                   uri = tBase + includeFile;
               }
               if (includeFiles.containsKey(uri)) {
                   inc =  includeFiles.get(uri);
               }
               // synchronously load a resource
               if (includeFile.startsWith("http")) {
                   IHttpClient hc = getHttpClient(includeFile);
                   InputStream is = hc.getInputStream();
                   StringBuffer buff = null;
                   buff = IOUtil.loadStringFromInputStream(is,getEncoding());
                   inc = new IncludeFile(uri, buff);
                   includeFiles.put(includeFile, inc);
                   return inc;
               }
               if (inc == null || (inc != null && inc.isStale(ctx))) {

                   StringBuffer buff = null;
                   buff = ctx.getResource(tBase, includeFile);
                   if (inc == null) {
                       inc = new IncludeFile(uri, buff);
                       if (inc.getTimeout() != null) {
                           inc.setTimeout(prop.getTimeout());
                       } else {
                           inc.setTimeout(0L);
                       }
                       if (prop.getDefer() != null) {
                           inc.setDefer(prop.getDefer());
                       }
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
           getLogger().log(Level.SEVERE, "Error getting include file content for template " +
                           template.getId() + " resource " + id + ".", e);
       }

       return null;
   }

   static void extendTemplate(int type, JSONObject bsjo, ITemplate temp, String baseURI) {

   }

   static void processURIResources(int type, JSONObject bsjo, ITemplate temp,
           String baseURI) throws JSONException {

       List<ResourceURI> refs = null;
       refs = new ArrayList<ResourceURI>();

       if (bsjo.has("libs")) {

           JSONArray ja = bsjo.getJSONArray("libs");

           for (int j = 0; j < ja.length(); j++) {

               JSONObject so = ja.getJSONObject(j);
               String url = so.getString("url");
               if (url.startsWith("/") || url.startsWith("http")) {
                   baseURI = "";
               }
               ResourceURI ri = new ResourceURI(url, baseURI, type);
               if (so.has("id")) {
                   ri.setId(so.getString("id"));
               }
               if (so.has("uaTest")) {
                   ri.setUATest(so.getString("uaTest"));
               }
               if (so.has("test")) {
                   ri.setTest(so.getString("test"));
               }
               if (so.has("defer")) {
                   ri.setDefer(so.getBoolean("defer"));
               }
               if (so.has("combine")) {
                   ri.setCombine(so.getBoolean("combine"));
               }
               if (so.has("uniqueURL")) {
                   Boolean unique = so.getBoolean("uniqueURL");
                   ri.setUniqueURL(unique);
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

   private void overrideTemplate( ITemplate parent,JSONArray templates, String baseURI ) {
       ITemplate temp = new ExtendedTemplate(this, parent, baseURI);
   }

   public void registerTemplates( JSONArray templates, String baseURI ) {

       for (int i=0; i < templates.length(); i++) {
           try {
               JSONObject t = templates.getJSONObject(i);
               String id = t.getString("id");
               ITemplate temp = new Template(id, baseURI, t, this);
               registerTemplates( temp, t, baseURI );
               tmap.put( id, temp);
               getLogger().info("Added template definition : " + temp.getId());

           } catch (JSONException e) {
               getLogger().log(Level.SEVERE, "Error parsing configuration.", e);
           }
       }
   }

   @SuppressWarnings("unchecked")
   private void registerTemplates( ITemplate temp, JSONObject t, String baseURI) {

           try {

                  if (t.has("timeout")) {
                      long templateTimeout = t.getLong("timeout");
                      temp.setTimeout(templateTimeout);
                  }

                  boolean tgzip = false;

                  if (!devMode) {
                      tgzip = gzip;
                  }

                  if (t.has("gzip")) {
                      tgzip = t.getBoolean("gzip");
                      temp.setGzipStyles(tgzip);
                      temp.setGzipScripts(tgzip);
                      temp.setGzipTemplate(tgzip);
                  }

                  if (t.has("uniqueURL")) {
                      Boolean unique = t.getBoolean("uniqueURL");
                      temp.setUniqueURL(unique);
                  }

                  // template overrides default combineResources
                  if (t.has("combineResources")) {
                      boolean combineResources = t.getBoolean("combineResources");
                      temp.setCombineResources(combineResources);
                      temp.setCombineScripts(combineResources);
                      temp.setCombineStyles(combineResources);
                  }

               if (t.has("template")) {
                   String turi = t.getString("template");
                   ResourceURI templateURI = new ResourceURI(turi, baseURI, ResourceURI.TEMPLATE);
                   temp.setTemplateURI(templateURI);
               }

               if (t.has("namespace")) {
                   temp.setURINamespace(t.getString("namespace"));
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
               
               if ( t.has( "overrides") ) {
                   System.out.println("We have overriders!");
                   List<TemplateOverride> overrides = new ArrayList<TemplateOverride>();
                   JSONArray joa = t.getJSONArray( "overrides" );
                   for (int z=0; z < joa.length(); z++) {
                       TemplateOverride tor = new TemplateOverride();
                       JSONObject toro = joa.getJSONObject( z );
                       if  ( toro.has("test") ) {
                           tor.setTest( toro.getString( "test") );
                       }
                       if ( toro.has( "uaTest") ) {
                           tor.setUATest( toro.getString( "uaTest" ));
                       }
                       if ( toro.has( "import") ) {
                           tor.setImportURI( toro.getString( "import") );
                       }
                       overrides.add( tor );
                       System.out.println("****** added " + tor );
                   }
                   temp.setTemplateOverrides( overrides );
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

                       IProperty pi= new Property(name, value, type, baseURI, temp.getId() );

                       if (so.has("timeout")) {
                           long timeout = so.getLong("timeout");
                           pi.setTimeout(timeout);
                       }
                       if (so.has("id")) {
                           pi.setId(so.getString("id"));
                       }
                       if (so.has("uaTest")) {
                           pi.setUATest(so.getString("uaTest"));
                       }
                       if (so.has("test")) {
                           pi.setTest(so.getString("test"));
                       }
                       if (so.has("defer")) {
                           pi.setDefer(so.getBoolean("defer"));
                       }
                       if (so.has("deferContent")) {
                           pi.setDeferContent(new StringBuffer(so.getString("deferContent")));
                       }
                       properties.put(name, pi);
                   }
                   temp.setProperties(properties);
               }

            } catch (JSONException e) {
               getLogger().log(Level.SEVERE, "Error parsing configuration.", e);
           }

   }

   public ITemplate getTemplate(String id, IContext ctx) {
       if (tmap.containsKey(id)) {
           ITemplate t = tmap.get(id);
           if ( t.getTemplateOverrides() != null) {
               System.out.println("@@@@@@@@@request for an overridden template");
           }
           return t;
       }
       return null;
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

   public IProperty getProperty(String tid, String id, IContext ctx) {
       ITemplate template = ctx.getTemplate();
       IProperty p = null;
       if (template != null) {
            p = template.getProperty(id, ctx);
       }
       return p;
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

   public String getMediaType() {
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

    @SuppressWarnings("unchecked")
    public IEngine getEngine() {
        if (engine == null) {
            Class<IEngine> clazz = null;
            try {
                clazz = (Class<IEngine>) Class.forName(engineClassName);
                engine = clazz.newInstance();
            } catch (ClassNotFoundException cnfe) {
                getLogger().severe("Fatal Error: Unable to find engine class " + engineClassName);
                throw new RuntimeException("Fatal Error: Unable to find engine class " + engineClassName);
            } catch (InstantiationException e) {
                getLogger().log(Level.SEVERE, "Fatal Error: Instantiation exception for engine class " + engineClassName, e);
                throw new RuntimeException("Fatal Error: Instantiation exception for engine class " + engineClassName, e);
            } catch (IllegalAccessException e) {
                getLogger().log(Level.SEVERE, "Fatal Error: Unable to access engine class " + engineClassName, e);
                throw new RuntimeException("Fatal Error: Unable to access engine class " + engineClassName, e);
            }
        }
        return engine;
    }

    public void setEngineClassName(String engineClassName) {
        this.engineClassName = engineClassName;
    }

    @SuppressWarnings("unchecked")
    public IHttpClient getHttpClient(String url) {
        IHttpClient hc = null;
        Class<IHttpClient> clazz = null;
        try {
            clazz = (Class<IHttpClient>) Class.forName(httpClientClassName);
            hc = clazz.newInstance();
        } catch (ClassNotFoundException cnfe) {
            getLogger().severe("Fatal Error: Unable to find engine class " + httpClientClassName);
            throw new RuntimeException("Fatal Error: Unable to find http client class " + httpClientClassName);
        } catch (InstantiationException e) {
            getLogger().log(Level.SEVERE, "Fatal Error: Instantiation exception for http class " + httpClientClassName, e);
            throw new RuntimeException("Fatal Error: Instantiation exception for http class " + httpClientClassName, e);
        } catch (IllegalAccessException e) {
            getLogger().log(Level.SEVERE, "Fatal Error: Unable to access http class " + httpClientClassName, e);
            throw new RuntimeException("Fatal Error: Unable to access http class " + httpClientClassName, e);
        }
        hc.setURL(url);
        return hc;
    }

    public void setProfile(boolean b) {
        this.profile = b;
    }

    public static String generateUUId() {
        return  System.currentTimeMillis() + "-" + counter++;
    }

}
