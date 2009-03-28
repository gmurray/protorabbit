package org.protorabbit.model.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.protorabbit.Config;
import org.protorabbit.accelerator.CacheableResource;
import org.protorabbit.model.ICommand;
import org.protorabbit.model.IContext;
import org.protorabbit.model.IProperty;
import org.protorabbit.model.ITemplate;
import org.protorabbit.model.ResourceURI;

public class TemplateImpl implements ITemplate {
	
	private ResourceURI templateURI = null;
	private JSONObject json = null;
	private String id = null;
	private StringBuffer contents = null;
	private List<ICommand>  commands = null;
    private List<String> ancestors = null;
    private Map<String, IProperty> properties = null;
    private List<ResourceURI> scripts = null;
    private List<ResourceURI> styles = null;
	private String baseURI = null;
	private boolean combineResources;
	private boolean gzipResources;
	private Config config = null;
	private long lastUpdate;
	private long timeout = 0;
	private boolean combineStyles = false;
	private boolean combineScripts = false;
	private CacheableResource templateResource = null;
	
	public TemplateImpl(String id, String baseURI, JSONObject json, Config cfg) {
		this.json = json;
		this.id = id;
		this.baseURI  = baseURI;
		properties = new HashMap<String, IProperty>();
		this.config  = cfg;
	}

	public void setContent(StringBuffer contents) {
		this.contents = contents;
	}

	public StringBuffer getContent(IContext ctx) {
			ResourceURI tri = getTemplateURI();
			long now = (new Date()).getTime();

			if (tri == null) {
				
				String message = "Unable to locate template for " + id;
				return new StringBuffer(message);
			} else {
				boolean needsUpdate = false;
				boolean isUpdated = false;
				if (ctx.getConfig().getDevMode()) {
					isUpdated = ctx.isUpdated(tri.getBaseURI() + tri.getUri(), lastUpdate);
				}
				if (now - lastUpdate > timeout)  {
					needsUpdate = true;
				}
                if (needsUpdate || isUpdated) {
					try {
						contents = ctx.getResource(tri.getBaseURI(), tri.getUri());
						lastUpdate = (new Date()).getTime();
					} catch (IOException e) {
						System.out.println("Error retrieving template with baseURI of " + tri.getBaseURI() + " and name " + tri.getUri());
						System.out.println("e");
						String message = "Error retriving template " + id + " please see log files for more details.";
						contents = new StringBuffer(message);
					}
				}
			}
		return contents;
	}

	public String getId() {
		return id;
	}

	public JSONObject getJSON() {
		return json;
	}

	public List<ICommand> getCommands() {
		return commands;
	}
	
	public void setCommands(List<ICommand> commands) {
		this.commands = commands;		
	}

	public List<String> getAncestors() {
		return ancestors;
	}	
	
	public void setAncestors(List<String> ancestors) {
		this.ancestors = ancestors;
	}

	public Map<String, IProperty> getProperties() {
		return properties;
	}

	/**
	 * Look locally for the property then at all the ancestors
	 */
	public IProperty getProperty(String id) {
		if (properties.containsKey(id)) {
		   return properties.get(id);
		}
		if (ancestors == null) {
			return null;
		}
		// check ancestors
		Iterator<String> it = ancestors.iterator();
		while (it.hasNext()) {
			ITemplate t = config.getTemplate(it.next());
			IProperty p = t.getProperty(id);
			if (p != null) {
				// add property to the local cache
				properties.put(id, p);
				return p;
			}
		}
		return null;
	}
	public List<ResourceURI> getScripts() {
		return scripts;
	}

	/*
	 *  Returns all scripts of this template and it's ancestors as ResourceURI objects
	 *  This method also insures that no duplicate references are returned and that 
	 *  children take preference over the ancestors.
	 *
	 */
	public List<ResourceURI> getAllScripts() {
		HashMap<String, String> existingRefs = new HashMap<String, String> ();
		
		List<ResourceURI> ascripts = new ArrayList<ResourceURI>();
		if (scripts != null) {
			Iterator<ResourceURI> sit = scripts.iterator();
			while (sit.hasNext()) {
				ResourceURI ri = sit.next();
				String id = ri.getId();
				if (!existingRefs.containsKey(id)) {
					ascripts.add(ri);
					existingRefs.put(id, "");
				}
			}
		}
		if (ancestors != null) {
			Iterator<String> it = ancestors.iterator();
			while (it.hasNext()) {
				ITemplate p = config.getTemplate(it.next());
				if (p != null) {
					List<ResourceURI>pscripts = p.getAllScripts();
					Iterator<ResourceURI> pit = pscripts.iterator();
					while (pit.hasNext()) {
						ResourceURI ri = pit.next();
						String id = ri.getId();
						if (!existingRefs.containsKey(id)) {
							ascripts.add(ri);
						}
					}
				}
			}
		}
		return ascripts;
	}
	
	public ResourceURI getTemplateURI() {
		if (templateURI != null) {
			return templateURI;
		}
		if (ancestors != null) {
			Iterator<String> it = ancestors.iterator();
			while (it.hasNext()) {
				ITemplate p = config.getTemplate(it.next());
				if (p != null) {
					
					if (p.getTemplateURI() != null) {
					
						return p.getTemplateURI() ;
					}
				}
		}
		}
		return null;
	}
	
	public List<ResourceURI> getAllStyles() {
		HashMap<String, String> existingRefs = new HashMap<String, String> ();
		
		List<ResourceURI> astyles = new ArrayList<ResourceURI>();
		if (styles != null) {
			Iterator<ResourceURI> sit = styles.iterator();
			while (sit.hasNext()) {
				ResourceURI ri = sit.next();
				String id = ri.getId();
				if (!existingRefs.containsKey(id)) {
					astyles.add(ri);
					existingRefs.put(id, "");
				}
			}
		}
		if (ancestors != null) {
			Iterator<String> it = ancestors.iterator();
			while (it.hasNext()) {
				ITemplate p = config.getTemplate(it.next());
				if (p != null) {
					List<ResourceURI>pstyles = p.getAllStyles();
					Iterator<ResourceURI> pit = pstyles.iterator();
					while (pit.hasNext()) {
						ResourceURI ri = pit.next();
						String id = ri.getId();
						if (!existingRefs.containsKey(id)) {
							astyles.add(ri);
						}
					}
				}
			}
		}
		return astyles;
	}


	public List<ResourceURI> getStyles() {
		return styles;
	}

	public void setProperty(String id, IProperty property) {
		properties.put(id, property);
		
	}

	public boolean combineResources() {
		return combineResources;
	}

	public String getBaseURI() {
		return baseURI;
	}

	public boolean gzipResources() {
		return gzipResources;
	}

	public void setScripts(List<ResourceURI> scripts) {
		this.scripts = scripts;		
	}

	public void setStyles(List<ResourceURI> styles) {
		this.styles = styles;		
	}

	public void setProperties(Map<String, IProperty> properties) {
		this.properties = properties;		
	}
	
	public void setTemplateURI(ResourceURI ri) {
		this.templateURI = ri;	
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
		
	}
	
	public long getTimeout() {
		return timeout;
	}

	public void setCombineScripts(boolean combineResources) {
		this.combineScripts = combineResources;
	}
	
	public boolean getCombineScripts() {
		return combineScripts;
	}
	
	public boolean getCombineStyles() {
		return combineStyles;
	}

	public void setCombineStyles(boolean combineResources) {
		this.combineStyles = combineResources;
	}

	public void setTemplateResource(CacheableResource cr) {
		templateResource = cr;
		
	}

	public CacheableResource getTemplateResource() {
		return templateResource;
	}
}
