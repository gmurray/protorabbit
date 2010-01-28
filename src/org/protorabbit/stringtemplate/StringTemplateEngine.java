package org.protorabbit.stringtemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.protorabbit.model.IContext;
import org.protorabbit.model.IEngine;
import org.protorabbit.model.ITemplate;
import org.protorabbit.model.impl.ResourceURI;
import org.protorabbit.util.IOUtil;
import org.antlr.stringtemplate.StringTemplate;

public class StringTemplateEngine implements IEngine {

    /*
     * Render a template with the id tid, context, and given outputsteam
     */
    public void renderTemplate(String tid, IContext ctx,  OutputStream out) {
        ITemplate t = ctx.getConfig().getTemplate(tid, ctx);
        renderTemplate( t, ctx, out );
    }

    public static void renderTemplate(ITemplate t,  IContext ctx,  OutputStream out) {
        renderTemplateAndGetMetaData( t,   ctx,  out);
    }

    public static Map<Object,Object> renderTemplateAndGetMetaData(ITemplate t,  IContext ctx,  OutputStream out) {
        Map<Object,Object> metaData = null;
        ResourceURI uri = t.getTemplateURI(ctx);
        String baseURI = null;
        if ( uri != null) {
            baseURI = uri.getURI();
        } else {
            System.out.println("using tempalte id " + t.getId() );
            baseURI = t.getId();
        }
        String prefix = "";
        String baseTemplate = null;
        if ( t.getDocumentContext() != null ) {
            System.out.println("baseURI is " + baseURI );

            int lastPath = baseURI.lastIndexOf("/");

            if ( lastPath != -1 ) {
                prefix = baseURI.substring(0, lastPath + 1 );
                baseTemplate = baseURI.substring( lastPath + 1 );
            } else {
                baseTemplate = baseURI;
            }

            System.out.println("prefix=" + prefix );

        } else {
            System.out.println("***** else here");
            int lastPath = baseURI.lastIndexOf("/");

            if ( lastPath != -1 ) {
                prefix = baseURI.substring(0, lastPath + 1 );
                baseTemplate = baseURI.substring( lastPath + 1 );
            } else {
                baseTemplate = baseURI;
            }
        }
        if ( baseTemplate.endsWith(".st") ) {
            baseTemplate = baseTemplate.substring(0, baseTemplate.length() - 3 );
        }
        StringTemplate st2 = null;
        STGroupDynamic group = new STGroupDynamic( ctx, prefix );
        if ( t.getDocumentContext() != null ) { 
            if ( t.getDocumentContext().getDocument() != null ) {
                st2 = group.loadTemplate( t.getId(), t.getDocumentContext().getDocument() );
            } else {
                System.out.println("Given a empty doucment with id " + t.getId() );
            }
        } else {
            st2 = group.loadTemplate( baseTemplate );
        }
        if ( st2 != null) {

            st2.setAttribute("title", "gtitle" );
            
            // copy in the ctx props
            Set<String> set = ctx.getAttributes().keySet();
            Iterator<String> it = set.iterator();
            while ( it.hasNext() ) {
                String key = it.next();
                System.out.println("adding " + key + " ctx=" + ctx.getAttribute(key));
                if ( !key.startsWith("org.protorabbit.")) {
                    st2.setAttribute( key , ctx.getAttribute(key) );
                }
            }
            String result = st2.toString();
            // get the meta data
            metaData = st2.getPostProcessMetaData();
            System.out.println("Meta data is =" + metaData );
            System.out.println( st2.getGroup().getTemplateNames() );
            StringTemplate c1 = st2.getGroup().getInstanceOf( "header" );
            c1.toString();
            System.out.println("Meta data is =" + c1.getPostProcessMetaData() );
            ByteArrayInputStream bis = new ByteArrayInputStream( result.getBytes() );
            try {
                IOUtil.writeBinaryResource( bis, out );
            } catch (IOException e) {
    
                e.printStackTrace();
            }
        } else {
            System.out.println( "Could not find template " + t.getId() );
        }
        return metaData;
    }

}