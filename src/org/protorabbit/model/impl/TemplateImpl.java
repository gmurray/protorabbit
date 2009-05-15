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

import org.json.JSONObject;
import org.protorabbit.Config;
import org.protorabbit.accelerator.ICacheable;
import org.protorabbit.model.ICommand;
import org.protorabbit.model.IContext;
import org.protorabbit.model.IProperty;
import org.protorabbit.model.ITemplate;
import org.protorabbit.model.ITestable;

public class TemplateImpl implements ITemplate {

    private ResourceURI templateURI = null;
    private JSONObject json = null;
    private String id = null;
    private StringBuffer contents = null;
    private List<ICommand> commands = null;
    private List<String> ancestors = null;
    private Map<String, IProperty> properties = null;
    private List<ResourceURI> scripts = null;
    private List<ResourceURI> styles = null;
    private String baseURI = null;
    private Boolean combineResources = null;
    private Boolean gzip = null;
    private Boolean gzipStyles = null;
    private Boolean gzipScripts = null;
    private Config config = null;
    private long lastUpdate;
    private Long timeout = null;
    private Boolean combineStyles = null;
    private Boolean combineScripts = null;
    private ICacheable templateResource = null;
    private Boolean hasUADependencies = null;
    private String uriNamespace = null;

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

            if (tri == null) {
                String message = "Unable to locate template for " + id;
                return new StringBuffer(message);
            } else {
                if (requiresRefresh(ctx)) {
                    try {
                        contents = ctx.getResource(tri.getBaseURI(), tri.getUri());
                        lastUpdate = (new Date()).getTime();
                    } catch (IOException e) {
                        Config.getLogger().log(Level.SEVERE, "Error retrieving template with baseURI of " + tri.getBaseURI() + " and name " + tri.getUri(), e);
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
    public IProperty getProperty(String id, IContext ctx) {
 
        if (properties.containsKey(id)) {
           IProperty prop = properties.get(id);
           // only return if it matches the uaTest (or doesn't have test)
           if (includeResource(prop,ctx)) {
               return prop;
           }
           return null;
        }
        if (ancestors == null) {
            return null;
        }
        // check ancestors
        Iterator<String> it = ancestors.iterator();
        while (it.hasNext()) {
            ITemplate t = config.getTemplate(it.next());
            IProperty p = t.getProperty(id, ctx);
            if (p != null) {
                // add property to the local cache
                properties.put(id, p);
                // only return if it matches the uaTest (or doesn't have test)
                if (includeResource(p,ctx)) {
                    return p;
                }
                return null;
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
    public List<ResourceURI> getAllScripts(IContext ctx) {
        HashMap<String, String> existingRefs = new HashMap<String, String> ();

        List<ResourceURI> ascripts = new ArrayList<ResourceURI>();
        if (scripts != null) {
            int size = scripts.size() -1;
            for (int i = size; i >= 0; i-=1) {
                ResourceURI ri = scripts.get(i);
                String id = ri.getId();
                if (!existingRefs.containsKey(id)) {
                    if (includeResource(ri,ctx)) {
                        ascripts.add(ri);
                        existingRefs.put(id, "");
                    }
                }
            }
        }
        if (ancestors != null) {
            Iterator<String> it = ancestors.iterator();
            while (it.hasNext()) {
                ITemplate p = config.getTemplate(it.next());
                if (p != null) {
                    List<ResourceURI>pscripts = p.getAllScripts(ctx);
                    Iterator<ResourceURI> pit = pscripts.iterator();
                    while (pit.hasNext()) {
                        ResourceURI ri = pit.next();
                        String id = ri.getId();
                        if (!existingRefs.containsKey(id)) {
                            if (includeResource(ri,ctx)) {
                                ascripts.add(ri);
                            }
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
                        templateURI = p.getTemplateURI();
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

    public List<ResourceURI> getAllStyles(IContext ctx) {
        HashMap<String, String> existingRefs = new HashMap<String, String> ();

        List<ResourceURI> astyles = new ArrayList<ResourceURI>();
        if (styles != null) {
            int size = styles.size() -1;
            for (int i = size; i >= 0; i-=1) {
                ResourceURI ri = styles.get(i);
                String id = ri.getId();
                if (!existingRefs.containsKey(id)) {
                    if (includeResource(ri,ctx)) {
                        astyles.add(ri);
                        existingRefs.put(id, "");
                    }
                }
            }
        }
        if (ancestors != null) {
            Iterator<String> it = ancestors.iterator();
            while (it.hasNext()) {
                ITemplate p = config.getTemplate(it.next());
                if (p != null) {
                    List<ResourceURI>pstyles = p.getAllStyles(ctx);
                    Iterator<ResourceURI> pit = pstyles.iterator();
                    while (pit.hasNext()) {
                        ResourceURI ri = pit.next();
                        String id = ri.getId();
                        if (!existingRefs.containsKey(id)) {
                            if (includeResource(ri,ctx)) {
                                astyles.add(ri);
                            }
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

    public void setCombineScripts(Boolean combineScripts) {
        this.combineScripts = combineScripts;
    }

    public Boolean combineResources() {

        if (combineResources == null) {
            if (ancestors != null) {
                Iterator<String> it = ancestors.iterator();
                while (it.hasNext()) {
                    ITemplate p = config.getTemplate(it.next());
                    if (p != null) {
                        if (p.combineResources() != null) {
                            combineResources = p.combineResources();
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

    public Long getTimeout() {
        if (timeout == null) {
            if (ancestors != null) {
                Iterator<String> it = ancestors.iterator();
                while (it.hasNext()) {
                    ITemplate p = config.getTemplate(it.next());
                    if (p != null) {
                        if (p.getTimeout() != null) {
                            timeout = p.getTimeout();
                            break;
                        }
                    }
                }
            }
        }
        return timeout;
    }

    public Boolean getCombineScripts() {
            if (combineScripts == null) {
                if (ancestors != null) {
                    Iterator<String> it = ancestors.iterator();
                    while (it.hasNext()) {
                        ITemplate p = config.getTemplate(it.next());
                        if (p != null) {
                            if (p.getCombineScripts() != null) {
                                combineScripts = p.getCombineScripts();
                                break;
                            }
                        }
                    }
                }
                if (combineScripts == null) {
                    combineScripts = combineResources();
                }
        }
        return combineScripts;
    }
    
    public Boolean getCombineStyles() {
        if (combineStyles == null) {
            if (ancestors != null) {
                Iterator<String> it = ancestors.iterator();
                while (it.hasNext()) {
                    ITemplate p = config.getTemplate(it.next());
                    if (p != null) {
                        if (p.getCombineStyles() != null) {
                            combineStyles = p.getCombineStyles();
                            break;
                        }
                    }
                }
            }
            if (combineStyles == null) {
                combineStyles = combineResources();
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
        ResourceURI tri = getTemplateURI();
        long now = (new Date()).getTime();
        boolean needsUpdate = false;
        boolean isUpdated = false;
        // check against the file resource
        if (ctx.getConfig().getDevMode()) {
            String base = tri.getBaseURI();
            // if context root then don't include the base
            if (tri.getUri().startsWith("/")) {
                base = "";
            }
            isUpdated = ctx.isUpdated(base + tri.getUri(), lastUpdate);
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

    public Boolean gzipScripts() {
        if (gzipScripts == null) {
            if (ancestors != null) {
                Iterator<String> it = ancestors.iterator();
                while (it.hasNext()) {
                    ITemplate p = config.getTemplate(it.next());
                    if (p != null) {
                        if (p.gzipScripts() != null) {
                            gzipScripts = p.gzipScripts();
                            break;
                        }
                    }
                }
            }
        }
        return gzipScripts;
    }

    public Boolean gzipStyles() {
        if (gzipStyles == null) {
            if (ancestors != null) {
                Iterator<String> it = ancestors.iterator();
                while (it.hasNext()) {
                    ITemplate p = config.getTemplate(it.next());
                    if (p != null) {
                        if (p.gzipStyles() != null) {
                            gzipStyles = p.gzipStyles();
                            break;
                        }
                    }
                }
            }
        }
        return gzipStyles;
    }

    public Boolean gzipTemplate() {
        if (gzip == null) {
            if (ancestors != null) {
                Iterator<String> it = ancestors.iterator();
                while (it.hasNext()) {
                    ITemplate p = config.getTemplate(it.next());
                    if (p != null) {
                        if (p.gzipTemplate() != null) {
                            gzip = p.gzipTemplate();
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

    /*
     *  Find out if this template has any user agent dependencies
     * 
     */
    public boolean hasUserAgentDependencies(IContext ctx) {
        if (hasUADependencies == null) {
            checkForUADependencies(ctx);
        }
        return hasUADependencies.booleanValue();
    }

    boolean checkPropsListForUATests(Map<String, IProperty> properties) {
        if (properties == null) {
            return false;
        }
        Set<String> keys = properties.keySet();
        for (String  key : keys) {
            ITestable p = properties.get(key); 
            if (p != null && 
                p.getUATest() != null) {
                return true;
            }
        }
        return false;
    }
    
   boolean checkPropertiesForUA() {
        
       boolean _hasDependnencies = false;
       _hasDependnencies = checkPropsListForUATests(properties);
        if (ancestors == null) {
            return _hasDependnencies;
        }
        // check ancestors
        Iterator<String> it = ancestors.iterator();
        while (it.hasNext()) {
            ITemplate t = config.getTemplate(it.next());
            _hasDependnencies = checkPropsListForUATests(t.getProperties());
            if (_hasDependnencies) {
                return _hasDependnencies;
            }
        }
        return _hasDependnencies;
    }

    boolean checkListForUATests(List<ResourceURI> list) {
        if (list == null) {
            return false;
        }
        for (ITestable t : list) {
            if (t.getUATest() != null) {
                return true;
            }
        }
        return false;
    }
    
    /*
     *  Go through and check all properties and ResourceURI references for this
     *  template whether they have user agent references. If they have references
     *  then we have to re-render by request.
     */
    void checkForUADependencies(IContext ctx) {
        boolean _hasDependnencies = false;
        _hasDependnencies = checkListForUATests(styles);
        if (!_hasDependnencies) {
            _hasDependnencies = checkListForUATests(scripts);
        }
        if (ancestors != null && !_hasDependnencies) {
            Iterator<String> it = ancestors.iterator();
            while (it.hasNext()) {
                ITemplate p = config.getTemplate(it.next());
                if (p != null) {
                    List<ResourceURI>pstyles = p.getAllStyles(ctx);
                    List<ResourceURI>pscripts = p.getAllScripts(ctx);
                    _hasDependnencies = checkListForUATests(pstyles);
                    if (!_hasDependnencies) {
                        _hasDependnencies = checkListForUATests(pscripts);
                    }
                    if (_hasDependnencies) {
                        break;
                    }
                }
            }
        }
        if (!_hasDependnencies) {
            _hasDependnencies = checkPropertiesForUA();
        }
       hasUADependencies = new Boolean(_hasDependnencies);
    }

    public void setURINamespace(String namespace) {
        this.uriNamespace = namespace;
    }

    public String getURINamespace() {
        if (uriNamespace == null) {
            if (ancestors != null) {
                Iterator<String> it = ancestors.iterator();
                while (it.hasNext()) {
                    ITemplate p = config.getTemplate(it.next());
                    if (p != null) {
                        if (p.gzipTemplate() != null) {
                            uriNamespace = p.getURINamespace();
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

}
