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
import org.protorabbit.accelerator.CacheableResource;
import org.protorabbit.model.ICommand;
import org.protorabbit.model.IContext;
import org.protorabbit.model.IProperty;
import org.protorabbit.model.ITemplate;
import org.protorabbit.model.IUATestable;

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
    private boolean gzip = true;
    private boolean gzipStyles = true;
    private boolean gzipScripts = true;    
    private Config config = null;
    private long lastUpdate;
    private long timeout = 0;
    private boolean combineStyles = false;
    private boolean combineScripts = false;
    private CacheableResource templateResource = null;
    private Boolean hasUADependencies = null;

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
            Iterator<ResourceURI> sit = scripts.iterator();
            while (sit.hasNext()) {
                ResourceURI ri = sit.next();
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
                    
                        return p.getTemplateURI() ;
                    }
                }
        }
        }
        return null;
    }

    private boolean includeResource(IUATestable ri, IContext ctx) {
        if (ri == null) {
            return false;
        }
        
        // in cases where there is a uaTest on a url test
        boolean includeResource = true;
        if (ri.getUATest() != null) {
            // if the test matches then keep the resource
            includeResource = ctx.uaTest(ri.getUATest());
        }
        return includeResource;
    }
    
    public List<ResourceURI> getAllStyles(IContext ctx) {
        HashMap<String, String> existingRefs = new HashMap<String, String> ();
        
        List<ResourceURI> astyles = new ArrayList<ResourceURI>();
        if (styles != null) {
            Iterator<ResourceURI> sit = styles.iterator();
            while (sit.hasNext()) {
                ResourceURI ri = sit.next();
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

    public boolean combineResources() {
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

    public boolean requiresRefresh(IContext ctx) {
        ResourceURI tri = getTemplateURI();
        long now = (new Date()).getTime();
        boolean needsUpdate = false;
        boolean isUpdated = false;
        // check against the file resource
        if (ctx.getConfig().getDevMode()) {
            isUpdated = ctx.isUpdated(tri.getBaseURI() + tri.getUri(), lastUpdate);
        }
        // check against the timeout
        if (now - lastUpdate > timeout)  {
             needsUpdate = true;
        }
        return needsUpdate || isUpdated;
    }

    public void setGzipScripts(boolean gzip) {
        gzipScripts = gzip;
    }

    public void setGzipStyles(boolean gzip) {
        gzipStyles = gzip;
    }

    public boolean gzipScripts() {
        return gzipScripts;
    }

    public boolean gzipStyles() {
        return gzipStyles;
    }

    public boolean gzipTemplate() {
        return gzip;
    }

    public void setGzipTemplate(boolean gzip) {
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
            IUATestable p = properties.get(key); 
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
        for (IUATestable t : list) {
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

}
