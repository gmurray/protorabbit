package org.protorabbit.model;

import java.util.Map;
import java.util.List;

import org.json.JSONObject;
import org.protorabbit.accelerator.CacheableResource;

public interface ITemplate {

	public String getBaseURI();
	
	public boolean combineResources();
	
	public boolean gzipResources();

	public String getId();
	
	public JSONObject getJSON();
	
	public List<String> getAncestors();
	
	public void setAncestors(List<String> ancestors);
	
	public List<ICommand> getCommands();
	
	public void setCommands(List<ICommand> commands);
	
	public StringBuffer getContent(IContext ctx);
	
	public void setContent(StringBuffer contents);
	
	public IProperty getProperty(String id);
	
	public List<ResourceURI> getAllScripts();
	public List<ResourceURI> getAllStyles();
	
	public ResourceURI getTemplateURI();
	public void setTemplateURI(ResourceURI ri);
	
	public List<ResourceURI> getScripts();
	public List<ResourceURI> getStyles();
	
	public Map<String,IProperty> getProperties();
	
	public void setProperty(String id, IProperty property);

	public void setStyles(List<ResourceURI> styles);

	public void setScripts(List<ResourceURI> scripts);

	public void setProperties(Map<String, IProperty> properties);

	public void setTimeout(long templateTimeout);
	public long getTimeout();

	public void setCombineScripts(boolean combineResources);
	public void setCombineStyles(boolean combineResources);

	public boolean getCombineScripts();
	public boolean getCombineStyles();

	public void setTemplateResource(CacheableResource cr);
	public CacheableResource getTemplateResource();
}
