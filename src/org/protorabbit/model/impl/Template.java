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

package org.protorabbit.model.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.protorabbit.Config;
import org.protorabbit.accelerator.ICacheable;
import org.protorabbit.json.Serialize;
import org.protorabbit.model.ICommand;
import org.protorabbit.model.IContext;
import org.protorabbit.model.IProperty;
import org.protorabbit.model.ITemplate;
import org.protorabbit.model.ITestable;

public class Template implements ITemplate {

    protected String id = null;
    private ResourceURI templateURI = null;
    private Boolean uniqueURL = null;
    private String uriNamespace = null;
    private Boolean combineResources = null;
    private Boolean combineStyles = null;
    private Boolean combineScripts = null;
    private Boolean gzipStyles = null;
    private Boolean gzipScripts = null;
    private Boolean gzip = null;
    private Long timeout = null;

    protected String baseURI = null;
    protected JSONObject json = null;

    protected StringBuffer contents = null;
    protected List<ICommand> commands = null;
    protected List<String> ancestors = null;
    protected Map<String, IProperty> properties = null;
    protected List<ResourceURI> scripts = null;
    protected List<ResourceURI> styles = null;

    protected Boolean hasUAScriptTests = null;
    protected Boolean hasUAStyleTests = null;


    protected Config config = null;
    protected long lastUpdate;
    protected long created = 0;


    protected ICacheable templateResource = null;
    protected Map<String, Object> attributes = null;
    protected DocumentContext dc = null;
    protected long accessCount = 0;
    protected List<TemplateOverride> overrides = null;

    protected static Logger logger = Logger.getLogger("org.protrabbit");


    public Template() {};

    public Template(String id, String baseURI, JSONObject json, Config cfg) {

        this.json = json;
        this.id = id;
        this.baseURI  = baseURI;
        properties = new HashMap<String, IProperty>();
        attributes = new HashMap<String, Object>();
        this.config  = cfg;
        this.created = System.currentTimeMillis();
    }

    static Logger getLogger() {
        return logger;
    }

    public void setContent(StringBuffer contents) {
        this.contents = contents;
    }

    public long getCreateTime() {
        return created;
    }

    @Serialize("skip")
    public StringBuffer getContent(IContext ctx) {
            ResourceURI tri = getTemplateURI( ctx );

            if (tri == null) {
                String message = "Unable to locate template for " + id;
                return new StringBuffer(message);
            } else {
                if (requiresRefresh(ctx)) {
                    try {
                        contents = ctx.getResource(tri.getBaseURI(), tri.getURI(false));
                        lastUpdate = (new Date()).getTime();
                    } catch (IOException e) {
                        getLogger().log(Level.SEVERE, "Error retrieving template with baseURI of " + tri.getBaseURI() + " and name " + tri.getURI(null), e);
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

    @Serialize("skip")
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
    public IProperty getProperty(String id, IContext ctx) {
        return getProperty(id, ctx, ancestors, properties);
    }
    /**
     * Look locally for the property then at all the ancestors
     */
    protected IProperty getProperty(String id, IContext ctx, List<String> _ancestors,  Map<String,IProperty> _properties) {
 
        if ( _properties.containsKey(id)) {
           IProperty prop = _properties.get(id);
           // only return if it matches the uaTest (or doesn't have test)
           if (includeResource(prop,ctx)) {
               return prop;
           }
           return null;
        }
        if (_ancestors == null) {
            return null;
        }
        // check ancestors
        Iterator<String> it = _ancestors.iterator();
        while (it.hasNext()) {
            ITemplate t = config.getTemplate(it.next(), ctx);
            IProperty p = t.getProperty(id, ctx);
            if (p != null) {
                // add property to the local cache
                _properties.put(id, p);
                // only return if it matches the uaTest (or doesn't have test)
                if (includeResource(p,ctx)) {
                    return p;
                }
                return null;
            }
        }
        return null;
    }

    /**
     * Look locally for a property with a given id. If not found check all ancestors.
     * 
     * This lets us abstract the properties that make up a template from the names used
     * internally for the templates.
     * 
     */
    public IProperty getPropertyById(String id, IContext ctx){
        return getPropertyById( id, ctx, properties, ancestors );
    }

    protected IProperty getPropertyById(String id, IContext ctx, Map<String,IProperty> _properties, List<String> _ancestors ) {
        for (String key : _properties.keySet()) {
            IProperty property = _properties.get(key);
            if (id.equals(key) || id.equals(property.getId())) {
                return property;
            }
        }
        if ( _ancestors == null) {
            return null;
        }
        // check ancestors
        Iterator<String> it = _ancestors.iterator();
        while (it.hasNext()) {
            ITemplate t = config.getTemplate( it.next(), ctx);
            IProperty p = t.getPropertyById(id, ctx);
            if (p != null) {
                return p;
            }
        }
        return null;
    }

    public void getDeferProperties( List<IProperty> dprops, IContext ctx){
        getDeferProperties( dprops, ctx, ancestors, properties );
    }

    protected void getDeferProperties(List<IProperty> dprops, IContext ctx, List<String> _ancestors,  Map<String,IProperty> _properties ) {
        for (String key : _properties.keySet()) {
            IProperty p = _properties.get(key);
            if (p.getDefer() != null && p.getDefer().booleanValue() == true) {
                dprops.add(p);
            }
        }
        if (_ancestors != null) {
            // check ancestors
            Iterator<String> it = _ancestors.iterator();
            while (it.hasNext()) {
                ITemplate t = config.getTemplate( it.next(), ctx );
                t.getDeferProperties(dprops, ctx);
            }
        }
    }

    public List<ResourceURI> getScripts() {
        return scripts;
    }

    public ResourceURI getTemplateURI( IContext ctx ) {
        if (templateURI != null) {
            return templateURI;
        }
        if (ancestors != null) {
            Iterator<String> it = ancestors.iterator();
            while (it.hasNext()) {
                ITemplate p = config.getTemplate(it.next(), ctx);
                if (p != null) {
                    if (p.getTemplateURI( ctx ) != null) {
                        templateURI = p.getTemplateURI( ctx );
                        break;
                    }
                }
            }
        }
        return templateURI;
    }

    private boolean includeResource(ITestable ri, IContext ctx) {
        if (ri == null) {
            return false;
        }

        // in cases where there is a uaTest on a url test
        boolean includeResource = true;
        if (ri.getUATest() != null) {
            // if the test matches then keep the resource
            includeResource = ctx.uaTest(ri.getUATest());
        }
        // return false if the first test fails
        if (includeResource == false ) {
            return includeResource;
        }
        // check the test if there is one
        if (ri.getTest() != null) {
            includeResource = ctx.test(ri.getTest());
        }
        return includeResource;
    }

    protected void inherits(IContext ctx,
                            HashMap<String, ResourceURI> existingRefs,
                            List<ResourceURI> astyles,
                            List<ResourceURI>pstyles) {

        Iterator<ResourceURI> pit = pstyles.iterator();
        while (pit.hasNext()) {
            ResourceURI ri = pit.next();
            String id = ri.getId();
            if (!existingRefs.containsKey(id)) {
                if (includeResource(ri, ctx)) {
                    // reset the written flag
                    ri.setWritten(false);
                    astyles.add(ri);
                    existingRefs.put(id, ri);
                }
            }
        }

    }

    enum ResourceType {
        SCRIPT,
        STYLE
    }

    protected List<ResourceURI> getAllResources(IContext ctx, List<String> _ancestors, List<ResourceURI> _resources, 
            ResourceType type) {

        HashMap<String, ResourceURI> existingRefs = new HashMap<String, ResourceURI> ();

        List<ResourceURI> _rlist = new ArrayList<ResourceURI>();

        if (_ancestors != null) {
            int size = _ancestors.size() -1;
            for (int i = size; i >= 0; i-=1) {
                String ancestorId = _ancestors.get(i);
                ITemplate p = config.getTemplate(ancestorId,ctx);
                if (p != null) {
                    List<ResourceURI>pstyles = null;
                    if (type == ResourceType.STYLE ){
                        pstyles = p.getAllStyles(ctx);
                    } else if (type == ResourceType.SCRIPT ) {
                        pstyles = p.getAllScripts(ctx);
                    }
                    inherits( ctx,existingRefs, _rlist, pstyles);
                }
            }
        }
        if (_resources != null) {
            int size = _resources.size();
            for (int i = 0; i < size; i+=1) {
                ResourceURI ri = _resources.get(i);
                String id = ri.getId();
                if (includeResource(ri,ctx)) {
                    // template can override
                    if (existingRefs.containsKey(id)) {
                        _rlist.remove(existingRefs.get(id));
                    }
                    // reset the written flag
                    ri.setWritten(false);
                    _rlist.add(ri);
                    existingRefs.put(id, ri);
                }
            }
        }
        return _rlist;
    }
    
    public List<ResourceURI> getAllStyles(IContext ctx) {
        return getAllStyles( ctx, ancestors, styles);
    }

    public List<ResourceURI> getAllStyles(IContext ctx, List<String> _ancestors, List<ResourceURI> _styles) {
        return getAllResources( ctx, _ancestors, _styles, ResourceType.STYLE ); 
    }

    /*
     *  Returns all scripts of this template and it's ancestors as ResourceURI objects
     *  This method also insures that no duplicate references are returned and that 
     *  children take preference over the ancestors.
     *
     */
    public List<ResourceURI> getAllScripts(IContext ctx){
        return getAllScripts(ctx, ancestors, scripts);
    }

    public List<ResourceURI> getAllScripts(IContext ctx, List<String> _ancestors, List<ResourceURI> _scripts) {
        return getAllResources( ctx, _ancestors, _scripts, ResourceType.SCRIPT ); 
    }

    public synchronized void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public List<ResourceURI> getStyles() {
        return styles;
    }

    public boolean hasUAScriptTests(IContext ctx) {
        if ( hasUAScriptTests != null) {
            return hasUAScriptTests;
        }
        List<ResourceURI> list = getAllStyles(ctx);
        for (ResourceURI ri : list) {
            if (ri.getTest() != null) {
                hasUAScriptTests = new Boolean(true);
            }
        }
        if (hasUAScriptTests == null) {
            hasUAScriptTests = new Boolean(false);
        }
        return hasUAScriptTests;
    }

    public boolean hasUAStyleTests(IContext ctx) {
        if ( hasUAStyleTests != null) {
            return hasUAStyleTests;
        }
        List<ResourceURI> list = getAllStyles(ctx);
        for (ResourceURI ri : list) {
            if (ri.getTest() != null) {
                hasUAStyleTests = new Boolean(true);
            }
        }
        if (hasUAStyleTests == null) {
            hasUAStyleTests = new Boolean(false);
        }
        return hasUAStyleTests;
    }

    public void setProperty(String id, IProperty property) {
        properties.put(id, property);
    }

    public void setCombineScripts(Boolean combineScripts) {
        this.combineScripts = combineScripts;
    }

    public Boolean combineResources( IContext ctx ) {

        if (combineResources == null) {
            if (ancestors != null) {
                Iterator<String> it = ancestors.iterator();
                while (it.hasNext()) {
                    ITemplate p = config.getTemplate(it.next(), ctx);
                    if (p != null) {
                        if (p.combineResources( ctx ) != null) {
                            combineResources = p.combineResources( ctx );
                            break;
                        }
                    }
                }
            }
        }
        return combineResources;
    }

    public String getBaseURI() {
        return baseURI;
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

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public Long getTimeout( IContext ctx) {
        if (timeout == null) {
            if (ancestors != null) {
                Iterator<String> it = ancestors.iterator();
                while (it.hasNext()) {
                    ITemplate p = config.getTemplate(it.next(), ctx);
                    if (p != null) {
                        if (p.getTimeout(ctx ) != null) {
                            timeout = p.getTimeout( ctx );
                            break;
                        }
                    }
                }
            }
        }
        return timeout;
    }

    public Boolean getCombineScripts( IContext ctx) {
            if (combineScripts == null) {
                if (ancestors != null) {
                    Iterator<String> it = ancestors.iterator();
                    while (it.hasNext()) {
                        ITemplate p = config.getTemplate(it.next(), ctx);
                        if (p != null) {
                            if (p.getCombineScripts( ctx ) != null) {
                                combineScripts = p.getCombineScripts( ctx );
                                break;
                            }
                        }
                    }
                }
                if (combineScripts == null) {
                    combineScripts = combineResources( ctx );
                }
        }
        return combineScripts;
    }

    public Boolean getCombineStyles( IContext ctx ) {
        if (combineStyles == null) {
            if (ancestors != null) {
                Iterator<String> it = ancestors.iterator();
                while (it.hasNext()) {
                    ITemplate p = config.getTemplate(it.next(), ctx);
                    if (p != null) {
                        if (p.getCombineStyles( ctx ) != null) {
                            combineStyles = p.getCombineStyles( ctx );
                            break;
                        }
                    }
                }
            }
            if (combineStyles == null) {
                combineStyles = combineResources( ctx );
            }
        }
        return combineStyles;
    }


    public void setCombineStyles(Boolean combineStyles) {
        this.combineStyles = combineStyles;
    }

    public void setTemplateResource(ICacheable cr) {
        templateResource = cr;
    }

    public ICacheable getTemplateResource() {
        return templateResource;
    }

    public boolean requiresRefresh(IContext ctx) {
        if (dc != null ) {
            if (dc.requiresRefresh()) {
                return true;
            }
        }
        ResourceURI tri = getTemplateURI( ctx );
        long now = (new Date()).getTime();
        boolean needsUpdate = false;
        boolean isUpdated = false;
        // check against the file resource
        if (ctx.getConfig().getDevMode()) {
            String base = tri.getBaseURI();
            // if context root then don't include the base
            if (tri.getURI(null).startsWith("/")) {
                base = "";
            }
            isUpdated = ctx.isUpdated(base + tri.getURI(null), lastUpdate);
        }
        // check against the timeout
        long ltimeout = 0;
        if (timeout != null) {
            ltimeout = timeout.longValue();
        }
        if (now - lastUpdate > ltimeout)  {
             needsUpdate = true;
        }
        return needsUpdate || isUpdated;
    }

    public void setGzipScripts(Boolean gzip) {
        gzipScripts = gzip;
    }

    public void setGzipStyles(Boolean gzip) {
        gzipStyles = gzip;
    }

    public Boolean gzipScripts( IContext ctx) {
        if (gzipScripts == null) {
            if (ancestors != null) {
                Iterator<String> it = ancestors.iterator();
                while (it.hasNext()) {
                    ITemplate p = config.getTemplate(it.next(), ctx);
                    if (p != null) {
                        if (p.gzipScripts( ctx ) != null) {
                            gzipScripts = p.gzipScripts( ctx );
                            break;
                        }
                    }
                }
            }
        }
        return gzipScripts;
    }

    public Boolean gzipStyles( IContext ctx ) {
        if (gzipStyles == null) {
            if (ancestors != null) {
                Iterator<String> it = ancestors.iterator();
                while (it.hasNext()) {
                    ITemplate p = config.getTemplate( it.next(), ctx);
                    if (p != null) {
                        if (p.gzipStyles( ctx ) != null) {
                            gzipStyles = p.gzipStyles( ctx );
                            break;
                        }
                    }
                }
            }
        }
        return gzipStyles;
    }

    public Boolean gzipTemplate( IContext ctx ) {
        if (gzip == null) {
            if (ancestors != null) {
                Iterator<String> it = ancestors.iterator();
                while (it.hasNext()) {
                    ITemplate p = config.getTemplate( it.next(), ctx);
                    if (p != null) {
                        if (p.gzipTemplate( ctx ) != null) {
                            gzip = p.gzipTemplate( ctx );
                            break;
                        }
                    }
                }
            }
        }
        return gzip;
    }

    public void setGzipTemplate(Boolean gzip) {
        this.gzip = gzip;
    }

    boolean checkPropsListForUATests(Map<String, IProperty> properties, IContext ctx) {
        if (properties == null) {
            return false;
        }
        boolean _hasDependnencies = false;
        Set<String> keys = properties.keySet();
        for (String  key : keys) {
            ITestable p = properties.get(key); 
            if (p != null && 
                p.getUATest() != null) {
                ctx.addUAScriptTest(p.getUATest());
                _hasDependnencies = true;
            }
        }
        return _hasDependnencies;
    }

   public boolean hasUserAgentPropertyDependencies(IContext ctx) {

       boolean _hasDependnencies = false;
       _hasDependnencies = checkPropsListForUATests(properties, ctx);
        if (ancestors == null) {
            return _hasDependnencies;
        }
        // check ancestors
        Iterator<String> it = ancestors.iterator();
        while (it.hasNext()) {
            ITemplate t = config.getTemplate(it.next(), ctx );
            if (checkPropsListForUATests(t.getProperties() , ctx)) {
                _hasDependnencies = true;
            }
            if (_hasDependnencies) {
                return _hasDependnencies;
            }
        }
        return _hasDependnencies;
    }

    boolean checkListForUATests(List<ResourceURI> list, IContext ctx, int type) {
        if (list == null) {
            return false;
        }
        boolean hasTest = false;
        for (ITestable t : list) {
            if (t.getUATest() != null) {
                if (type == Config.SCRIPT) {
                    ctx.addUAScriptTest(t.getUATest());
                } else if (type == Config.STYLE) {
                    ctx.addUAStyleTest(t.getUATest());
                }
                hasTest = true;
            }
        }
        return hasTest;
    }

    /*
     *  Go through and check all properties and ResourceURI references for this
     *  template whether they have user agent references. If they have references
     *  then we have to re-render by request.
     */
    public boolean hasUserAgentScriptDependencies(IContext ctx) {
        boolean _hasDependnencies = false;
        if (checkListForUATests(scripts,ctx,Config.SCRIPT)) {
            _hasDependnencies = true;
        }
        if (ancestors != null) {
            Iterator<String> it = ancestors.iterator();
            while (it.hasNext()) {
                ITemplate p = config.getTemplate( it.next(), ctx);
                if (p != null) {
                    List<ResourceURI>pscripts = p.getAllScripts(ctx);
                    if (checkListForUATests(pscripts,ctx,Config.SCRIPT)) {
                        _hasDependnencies = true;
                    }
                }
            }
        }
        return _hasDependnencies;
    }

    /*
     *  Go through and check all properties and ResourceURI references for this
     *  template whether they have user agent references. If they have references
     *  then we have to re-render by request.
     */
    public boolean hasUserAgentStyleDependencies(IContext ctx) {
        boolean _hasDependnencies = false;
        if (checkListForUATests(styles,ctx,Config.STYLE)) {
            _hasDependnencies =  true;
        }
        if (ancestors != null) {
            Iterator<String> it = ancestors.iterator();
            while (it.hasNext()) {
                ITemplate p = config.getTemplate( it.next() ,ctx );
                if (p != null) {
                    List<ResourceURI>pstyles = p.getAllStyles(ctx);
                    if (checkListForUATests(pstyles, ctx,Config.STYLE)) {
                        _hasDependnencies = true;
                    };
                }
            }
        }
        return _hasDependnencies;
    }

    public void setURINamespace(String namespace) {
        this.uriNamespace = namespace;
    }

    public String getURINamespace( IContext ctx ) {
        if (uriNamespace == null) {
            if (ancestors != null) {
                Iterator<String> it = ancestors.iterator();
                while (it.hasNext()) {
                    ITemplate p = config.getTemplate( it.next(), ctx );
                    if (p != null) {
                        if (p.gzipTemplate( ctx ) != null) {
                            uriNamespace = p.getURINamespace( ctx );
                            break;
                        }
                    }
                }
            }
        }
        return uriNamespace;
    }

    public void setCombineResources(Boolean combineResources) {
        this.combineResources = combineResources;
    }

    public DocumentContext getDocumentContext() {
        return dc;
    }

    public void setDocumentContext(DocumentContext dc) {
        this.dc = dc;
    }

    public void setUniqueURL(Boolean uniqueURL) {
        this.uniqueURL  = uniqueURL;
    }

    public Boolean getUniqueURL( IContext ctx ) {
        if (uniqueURL == null && ancestors != null) {
            Iterator<String> it = ancestors.iterator();
            while (it.hasNext()) {
                String tid = it.next();
                ITemplate t = config.getTemplate( tid, ctx );
                if (t != null) {
                    Boolean u = t.getUniqueURL( ctx );
                    if (t != null) {
                        uniqueURL = u;
                    }
                } else {
                    getLogger().log( Level.WARNING, "Error retrieving ancestor template with id " + tid );
                }
            }
        }
        return uniqueURL;
    }

    public long getAccessCount() {
        return accessCount;
    }

    public void incrementAccessCount() {
        accessCount++;
    }

    public List<TemplateOverride> getTemplateOverrides() {
        return overrides;
    }

    public void setTemplateOverrides(List<TemplateOverride> overrides) {
        this.overrides = overrides;
    }

    public void setAttributes (Map<String,Object> attributes) {
        this.attributes = attributes;
    }

    public void destroy() {
        properties.clear();
        attributes.clear();
        properties = null;
        attributes = null;
        scripts.clear();
        styles.clear();
        templateResource = null;
    }

}
