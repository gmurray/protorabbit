package org.protorabbit.stringtemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateErrorListener;
import org.protorabbit.model.IContext;
import org.protorabbit.model.IEngine;
import org.protorabbit.model.ITemplate;
import org.protorabbit.model.impl.ResourceURI;
import org.protorabbit.util.IOUtil;

public class StringTemplateEngine implements IEngine {

    private static Logger logger = null;

    static final Logger getLogger() {
        if ( logger == null ) {
            logger = Logger.getLogger( "org.protrabbit" );
        }
        return logger;
    }

    /*
     * Render a template with the id tid, context, and given outputsteam
     */
    public void renderTemplate( String tid, IContext ctx,  OutputStream out) {
        ITemplate t = ctx.getConfig().getTemplate( tid, ctx );
        renderTemplate( t, ctx, out );
    }

    public static void renderTemplate( ITemplate t,  IContext ctx,  OutputStream out ) {
        renderTemplate( t, ctx,  out, true );
    }

    public static Map<Object,Object> getMetaData( ITemplate t, IContext ctx ) {
        return renderTemplate( t, ctx,  null, true );
    }

    @SuppressWarnings("unchecked")
    public static Map<Object,Object> renderTemplate( ITemplate t, IContext ctx, OutputStream out, boolean getMetaData ) {
        Map<Object,Object> metaData = null;
        ResourceURI uri = t.getTemplateURI(ctx);
        String baseURI = null;
        if ( uri != null) {
            baseURI = uri.getURI();
        } else {
            baseURI = t.getId();
        }
        if ( baseURI == null ) {
            throw new RuntimeException( "A templateId or templateURI is required for a tempalte to be rendered." );
        }
        String prefix = "";
        String baseTemplate = null;
        if ( t.getDocumentContext() != null ) {
            int lastPath = baseURI.lastIndexOf("/");
            if ( lastPath != -1 ) {
                prefix = baseURI.substring(0, lastPath + 1 );
                baseTemplate = baseURI.substring( lastPath + 1 );
            } else {
                baseTemplate = baseURI;
            }
        } else {
            int lastPath = baseURI.lastIndexOf("/");
            if ( lastPath != -1 ) {
                prefix = baseURI.substring(0, lastPath + 1 );
                baseTemplate = baseURI.substring( lastPath + 1 );
            } else {
                baseTemplate = baseURI;
            }
        }
        // bail out for empty docs.
        if ( (t != null && t.getDocumentContext() != null) && (t.getDocumentContext().getDocument() == null || 
             t.getDocumentContext().getDocument().length() == 0) ) {
            return null;
        }
        if ( baseTemplate.endsWith(".st") ) {
            baseTemplate = baseTemplate.substring(0, baseTemplate.length() - 3 );
        }
        StringTemplate st2 = null;
        STGroupDynamic group = new STGroupDynamic( ctx, prefix );
        // attach an error listener
        StringTemplateErrorListener setl = new STErrorProcessor();
        group.setErrorListener( setl );
        if ( t.getDocumentContext() != null ) { 
            if ( t.getDocumentContext().getDocument() != null ) {
                st2 = group.loadTemplate( t.getId(), t.getDocumentContext().getDocument() );
            } else {
                getLogger().log( Level.SEVERE, "Given a empty document with id " + t.getId() );
            }
        } else {
            st2 = group.loadTemplate( baseTemplate );
        }
        if ( st2 != null) {

            // copy in the ctx props
            Set<String> set = ctx.getAttributes().keySet();
            Iterator<String> it = set.iterator();
            Map<String,Object> atts = new HashMap<String,Object>();
            while ( it.hasNext() ) {
                String key = it.next();
                if ( !key.startsWith("org.protorabbit.")) {
                    atts.put( key , ctx.getAttribute(key) );
                }
            }
            st2.setAttributes( atts );
            // register out customized date renderer
            st2.registerRenderer( java.util.Date.class , new DateAttributeRenderer() );
            String result = st2.toString();
            // get the meta data
            if ( getMetaData ) {
                metaData = st2.getPostProcessMetaData();
                Set<String> templates = st2.getGroup().getTemplateNames();
                Iterator<String> myIt = templates.iterator();
                Map<String,Map<String,Object>> subtemplates = new HashMap<String,Map<String,Object>>();
                metaData.put("subTemplates", subtemplates );
                while ( myIt.hasNext() ) {
                    String templateName = myIt.next();
                    if ( templateName.equals( st2.getName() )) {
                        // skip the top level
                        continue;
                    }
                    StringTemplate _st = st2.getGroup().getInstanceOf( templateName );
                    if ( _st != null ) {
                        // mixin attribute set
                        _st.setAttributes( atts );
                        _st.toString();
                        Map<String, Object> _meta = _st.getPostProcessMetaData() ;
                        subtemplates.put( templateName, _meta );
                    }
                }
            }
            if ( out != null ) {
                ByteArrayInputStream bis = new ByteArrayInputStream( result.getBytes() );
                try {
                    IOUtil.writeBinaryResource( bis, out );
                } catch (IOException e) {
                    getLogger().log( Level.SEVERE, "Error rendering template " + t.getId(), e );
                }
            }
        } else {
            getLogger().log( Level.WARNING, "Could not find template " + t.getId() );
        }
        return metaData;
    }

}