package org.protorabbit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.protorabbit.accelerator.CombinedResourceManager;
import org.protorabbit.model.ICommand;
import org.protorabbit.model.IContext;
import org.protorabbit.model.IProperty;
import org.protorabbit.model.ITemplate;
import org.protorabbit.model.ResourceURI;
import org.protorabbit.model.impl.IncludeFile;
import org.protorabbit.model.impl.PropertyImpl;
import org.protorabbit.model.impl.TemplateImpl;

public class Config {
	
	public static long DEFAULT_TIMEOUT = 60 * 1000 * 15;

	private long resourceTimeout = DEFAULT_TIMEOUT;

	String encoding = "UTF-8";

	String defaultMediaType = "screen, projection";
	String commandBase = "";

	Map<String, ITemplate> tmap = null;
	Map<String, IncludeFile> includeFiles = null;
	Map<String, String> commandMap = null;
	
	boolean gzip = true;
	boolean devMode = false;
	
	CombinedResourceManager crm = null;
	
	long combinedResourceTimeout = 60 * 1000 + 60 * 24;

	// in seconds 
	private long maxAge = 1225000;
	

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
	    		                         "spv",
	    		                         getMaxAge());	    
	}
	
	void init() {
		
		commandMap = new HashMap<String, String>();
		commandMap.put("insert", "org.protorabbit.model.impl.InsertCommand");
		commandMap.put("include", "org.protorabbit.model.impl.IncludeCommand");
		commandMap.put("includeReferences", "org.protorabbit.model.impl.IncludeReferencesCommand");
		
		tmap = new HashMap<String, ITemplate>();
		includeFiles = new HashMap<String, IncludeFile>();
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
			    System.err.println("Error locating class impementation for command " + name + ".");
			    return null;
			}
		} else {
			try {
			    clazz = Class.forName(className);
			} catch (ClassNotFoundException cnfe) {
				System.out.println("Could not find class " + className);
				return null;
			}

		}

		try {

			Object o = clazz.newInstance();
			if (o instanceof ICommand) {
				ICommand ic = (ICommand)o;
				return ic;
			} else {
				System.err.println("Error creating instance of " + className + ". The command needs to implement org.protorabbit.model.Command");
			}
            

		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
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
	

	public StringBuffer getIncludeFileContent(String tid, String id, IContext ctx) {
		try {
			ITemplate template = getTemplate(tid);
			if (template != null && template.getJSON() != null) {
				
				IProperty prop = template.getProperty(id);
				if (prop == null) {
					System.out.println("Unable to find Include file for " + id + " in template " + tid);
					return new StringBuffer("");
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
						includeFiles.put(uri, inc);
					} else {
						inc.setContent(buff);
					}
					
					return buff;
				} else {
					return inc.getContent();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
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

			   	boolean combineResources = false;
			   	if (t.has("combineResources")) {
			   		combineResources = t.getBoolean("combineResources");
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
				    List<ResourceURI> scripts = null;			    	
			    	scripts = new ArrayList<ResourceURI>();
			    	JSONObject bsjo = t.getJSONObject("scripts");
			    	if (bsjo.has("combineResources")) {
						JSONArray ja = bsjo.getJSONArray("libs");
						for (int j=0; j < ja.length(); j++) {				
							// TODO : Do a user agent check on the test attribute if found
							JSONObject so = ja.getJSONObject(j);
							ResourceURI ri = new ResourceURI(so.getString("url"), baseURI, ResourceURI.SCRIPT);
							if (so.has("id")) {
								ri.setId(so.getString("id"));
							}
							scripts.add(ri);
						}
			    	}
				    boolean combine = combineResources;
				    if (bsjo.has("combineResources")) {
				    	combine = bsjo.getBoolean("combineResources");
				    } 
			    	temp.setCombineScripts(combine);					
				    temp.setScripts(scripts);
			    }

			    if (t.has("styles")) {
				    List<ResourceURI> styles = null;			    	
			    	styles = new ArrayList<ResourceURI>();
			    	JSONObject bsjo = t.getJSONObject("styles");
				    if (bsjo.has("libs")) {
						JSONArray ja = bsjo.getJSONArray("libs");
						for (int j=0; j < ja.length(); j++) {
							// TODO : Do a user agent check on the test attribute if found
							JSONObject so = ja.getJSONObject(j);
							ResourceURI ri = new ResourceURI(so.getString("url"), baseURI, ResourceURI.SCRIPT);
							if (so.has("id")) {
								ri.setId(so.getString("id"));
							}
							styles.add(ri);
						}
					    temp.setStyles(styles);
			    	}
				    boolean combine = combineResources;
				    if (bsjo.has("combineResources")) {
				    	combine = bsjo.getBoolean("combineResources");
				    } 
			    	temp.setCombineStyles(combine);
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
						pi.setTimeout(timeout);
						properties.put(name, pi);
					}
				    temp.setProperties(properties);
			    }

			   tmap.put(id, temp);
			   System.out.println("Added template definition :  " + id);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}	
	}
	
	public ITemplate getTemplate(String id) {
		if (tmap.containsKey(id)) {
			return tmap.get(id);
		}
		return null;
	}
	
	public String generateScriptReferences(ITemplate template, IContext ctx) {
		List<ResourceURI> scripts = template.getAllScripts();
		return generateReferences(template,ctx,scripts, ResourceURI.SCRIPT);	
	}
	
	public String generateStyleReferences(ITemplate template, IContext ctx) {
		List<ResourceURI> styles = template.getAllStyles();
		return generateReferences(template,ctx,styles, ResourceURI.LINK);		
	}
	
	public String generateReferences(ITemplate template, IContext ctx, List<ResourceURI> resources, int type) {
		String buff = "";		
        
            if (resources != null) {
                Iterator<ResourceURI> it = resources.iterator();
                while (it.hasNext()) {
                	
                	ResourceURI ri = it.next();
                	String resource = ri.getUri();
                	String baseURI =  ctx.getContextRoot();
                	
                    if (!ri.isExternal()){
               			baseURI +=  ri.getBaseURI();
                	} else {
                		baseURI = "";
                	}
                	if (type== ResourceURI.SCRIPT) {
                	    buff += "<script type=\"text/javascript\" src=\"" + baseURI + resource + "\"></script>\n";
                	} else if (type == ResourceURI.LINK) {
                		String mediaType = ri.getMediaType();
                		if (mediaType == null){
                			mediaType = defaultMediaType;
                		}
                		buff += "<link rel=\"stylesheet\" type=\"text/css\"  href=\"" + baseURI + resource + "\" media=\""  + mediaType + "\" />\n";
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
				e.printStackTrace();
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
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public String getContent(String tid, String id, IContext ctx) {
		ITemplate template = getTemplate(tid);
		if (template != null) {
			
			IProperty p = template.getProperty(id);
			if (p != null) {			
				return p.getValue();
			}

		}
		return null;
	}
	
	public String getResourceReferences(String tid, String pid, IContext ctx) {
		ITemplate template = getTemplate(tid);
		String rtype = pid.toLowerCase();
		if (template != null && "scripts".equals(rtype)) {
            return generateScriptReferences(template, ctx);
		} if (template != null && "styles".equals(rtype)) {
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
	
}
