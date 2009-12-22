package org.protorabbit.model.impl;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.protorabbit.Config;
import org.protorabbit.model.IContext;
import org.protorabbit.model.IProperty;
import org.protorabbit.model.ITemplate;


/*
 * This is a single use template that extends an existing template with extra properties
 * which will be used if a user agent test or a test passes
 */
public class ExtendedTemplate extends Template {

    private ITemplate parent = null;

    private static Logger logger = Logger.getLogger("org.protrabbit");

    public ExtendedTemplate( Config config, ITemplate parent, String baseURI ) {
        super();
        this.parent = parent;
        this.id = parent.getId();
        this.config = config;
        this.baseURI = baseURI;
        properties = new HashMap<String, IProperty>();
        attributes = new HashMap<String, Object>();
    }

    static Logger getLogger() {
        return logger;
    }

    public IProperty getProperty( String id, IContext ctx ) {
        IProperty prop = getProperty(id, ctx, ancestors, properties);
        if ( prop != null ) {
            return prop;
        } else {
            return parent.getProperty(id, ctx);
        }
    }

    public void getDeferProperties( List<IProperty> dprops, IContext ctx ){
        getDeferProperties( dprops, ctx, ancestors, properties );
        parent.getDeferProperties(dprops, ctx);
    }

    public ResourceURI getTemplateURI( IContext ctx ) {
        ResourceURI tri = super.getTemplateURI( ctx );
        if ( tri != null) {
            return tri;
        } else {
            return parent.getTemplateURI( ctx );
        }
    }

    public Boolean getUniqueURL( IContext ctx ) {
        Boolean unique = super.getUniqueURL( ctx );
        if ( unique == null) {
            return unique;
        } else {
            return parent.getUniqueURL( ctx );
        }
    }

    public List<ResourceURI> getAllScripts( IContext ctx ){
        // process our own
        List<ResourceURI> tlist = super.getAllScripts( ctx, ancestors, scripts );
        // add the parent's scripts
        return parent.getAllScripts( ctx, parent.getAncestors(), tlist);
    }

    public List<ResourceURI> getAllStyles(IContext ctx) {
        // process our own
        List<ResourceURI> tlist = super.getAllStyles( ctx, ancestors, styles );
        // add the parent's scripts
        return parent.getAllStyles( ctx, parent.getAncestors(), tlist);
    }
    public String getURINamespace( IContext ctx ) {
        String uri = super.getURINamespace( ctx );
        if ( uri != null ) {
            return uri;
        } else {
            return parent.getURINamespace( ctx );
        }
    }
    
    public Boolean getCombineStyles( IContext ctx ) {
        Boolean cs = super.getCombineStyles( ctx );
        if ( cs != null) {
            return cs;
        } else {
            return parent.getCombineStyles( ctx );
        }
    }

    public Boolean getCombineScripts( IContext ctx ) {
        Boolean cs = super.getCombineScripts( ctx );
        if ( cs != null) {
            return cs;
        } else {
            return parent.getCombineScripts( ctx );
        }
    }

    public Boolean combineResources( IContext ctx ) {
        Boolean cs = super.combineResources( ctx );
        if ( cs != null) {
            return cs;
        } else {
            return parent.combineResources( ctx);
        }
    }
    
    public Boolean gzipScripts( IContext ctx ) {
        Boolean gz = super.gzipScripts( ctx );
        if ( gz != null ) {
            return gz;
        } else {
            return parent.gzipScripts( ctx );
        }
    }

    public Boolean gzipStyles( IContext ctx ) {
        Boolean gz = super.gzipStyles( ctx );
        if ( gz != null ) {
            return gz;
        } else {
            return parent.gzipStyles( ctx );
        }
    }

    public Boolean gzipTemplate( IContext ctx ) {
        Boolean gz = super.gzipTemplate( ctx );
        if ( gz != null ) {
            return gz;
        } else {
            return parent.gzipTemplate( ctx );
        }
    }

    public Long getTimeout( IContext ctx ) {
        Long time = super.getTimeout( ctx );
        if ( time != null ) {
            return time;
        } else {
            return parent.getTimeout( ctx );
        }
    }

    public void destroy() {
        super.destroy();
    }

    // always need to refresh given this is always assumed to be stale
    public boolean requiresRefresh( IContext ctx) {
        return true;
    }
}
