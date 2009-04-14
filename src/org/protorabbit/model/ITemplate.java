package org.protorabbit.model;

import java.util.Map;
import java.util.List;

import org.json.JSONObject;
import org.protorabbit.accelerator.CacheableResource;
import org.protorabbit.model.impl.ResourceURI;

public interface ITemplate {

    public boolean requiresRefresh(IContext ctx);
    public String getBaseURI();
    public boolean combineResources();
    public String getId();
    public JSONObject getJSON();
    public List<String> getAncestors();
    public void setAncestors(List<String> ancestors);
    public List<ICommand> getCommands();
    public void setCommands(List<ICommand> commands);
    public StringBuffer getContent(IContext ctx);
    public void setContent(StringBuffer contents);
    public IProperty getProperty(String id, IContext ctx);
    public List<ResourceURI> getAllScripts(IContext ctx);
    public List<ResourceURI> getAllStyles(IContext ctx);
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
    public void setGzipTemplate(boolean gzip);
    public void setGzipScripts(boolean gzip);
    public void setGzipStyles(boolean gzip);
    public boolean gzipTemplate();
    public boolean gzipScripts();
    public boolean gzipStyles();
    public boolean hasUserAgentDependencies(IContext ctx);
}
