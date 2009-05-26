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

package org.protorabbit.model;

import java.util.Map;
import java.util.List;

import org.json.JSONObject;
import org.protorabbit.accelerator.ICacheable;
import org.protorabbit.model.impl.DocumentContext;
import org.protorabbit.model.impl.ResourceURI;

public interface ITemplate {

    public boolean requiresRefresh(IContext ctx);
    public String getBaseURI();
    public String getId();
    public JSONObject getJSON();
    public List<String> getAncestors();
    public void setAncestors(List<String> ancestors);
    public List<ICommand> getCommands();
    public void setCommands(List<ICommand> commands);
    public StringBuffer getContent(IContext ctx);
    public void setContent(StringBuffer contents);
    public IProperty getProperty(String id, IContext ctx);
    public IProperty getPropertyById(String id, IContext ctx);
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
    public void setTimeout(Long templateTimeout);
    public Long getTimeout();
    public void setTemplateResource(ICacheable cr);
    public ICacheable getTemplateResource();
    public void setGzipTemplate(Boolean gzip);
    public void setGzipScripts(Boolean gzip);
    public void setGzipStyles(Boolean gzip);
    public Boolean combineResources();
    public void setCombineResources(Boolean combineResources);
    public void setCombineScripts(Boolean combineResources);
    public void setCombineStyles(Boolean combineResources);
    public Boolean getCombineScripts();
    public Boolean getCombineStyles();
    public Boolean gzipTemplate();
    public Boolean gzipScripts();
    public Boolean gzipStyles();
    public String getURINamespace();
    public void setURINamespace(String namespace);
    public boolean hasUserAgentDependencies(IContext ctx);
    public void setAttribute(String key, Object value);
    public Object getAttribute(String key);
    public DocumentContext getDocumentContext();
    public void setDocumentContext(DocumentContext dc);
    public void getDeferProperties(List<IProperty> deferredProperties, IContext ctx);
}
