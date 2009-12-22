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
import org.protorabbit.model.impl.TemplateOverride;

public interface ITemplate {

    public String getBaseURI();
    public String getId();

    public Long getTimeout( IContext ctx );
    public JSONObject getJSON();
    public ResourceURI getTemplateURI( IContext ctx );
    public List<ResourceURI> getScripts();
    public List<ResourceURI> getStyles( );
    public List<TemplateOverride> getTemplateOverrides();
    public Map<String,IProperty> getProperties();
    public ICacheable getTemplateResource();
    public Boolean combineResources( IContext ctx);
    public Boolean getCombineScripts( IContext ctx);
    public Boolean getCombineStyles( IContext ctx);
    public Boolean gzipTemplate( IContext ctx);
    public Boolean gzipScripts( IContext ctx);
    public Boolean gzipStyles( IContext ctx);
    public String getURINamespace( IContext ctx);
    public Object getAttribute( String key);
    public DocumentContext getDocumentContext();
    public List<String> getAncestors();
    public List<ICommand> getCommands();

    public StringBuffer getContent( IContext ctx);
    public IProperty getProperty( String id, IContext ctx);
    public IProperty getPropertyById( String id, IContext ctx);
    public void getDeferProperties(List<IProperty> deferredProperties, IContext ctx);
    public List<ResourceURI> getAllScripts( IContext ctx);
    public List<ResourceURI> getAllStyles( IContext ctx);
    public boolean requiresRefresh( IContext ctx);
    public Boolean getUniqueURL(IContext ctx);

    public long getCreateTime();
    public long getAccessCount();
    public void incrementAccessCount();

    public void setURINamespace(String namespace);
    public void setDocumentContext(DocumentContext dc);
    public void setAttributes( Map<String, Object>attributes);
    public void setAttribute(String key, Object value);
    public void setCombineResources(Boolean combineResources);
    public void setCombineScripts(Boolean combineResources);
    public void setCombineStyles(Boolean combineResources);
    public void setGzipTemplate(Boolean gzip);
    public void setGzipScripts(Boolean gzip);
    public void setGzipStyles(Boolean gzip);
    public void setTemplateResource(ICacheable cr);
    public void setCommands(List<ICommand> commands);
    public void setAncestors(List<String> ancestors);
    public void setProperty(String id, IProperty property);
    public void setStyles(List<ResourceURI> styles);
    public void setScripts(List<ResourceURI> scripts);
    public void setProperties(Map<String, IProperty> properties);
    public void setTimeout(Long templateTimeout);
    public void setContent(StringBuffer contents);
    public void setTemplateURI(ResourceURI ri);
    public void setTemplateOverrides(List<TemplateOverride> overrides);
    public void setUniqueURL(Boolean tgzip);

    public boolean hasUserAgentScriptDependencies(IContext ctx);
    public boolean hasUserAgentStyleDependencies(IContext ctx);
    public boolean hasUserAgentPropertyDependencies(IContext ctx);
    public List<ResourceURI> getAllStyles(IContext ctx, List<String> ancestors, List<ResourceURI> tlist);
    public List<ResourceURI> getAllScripts(IContext ctx, List<String> ancestors, List<ResourceURI> tlist);
    public void destroy();
}
