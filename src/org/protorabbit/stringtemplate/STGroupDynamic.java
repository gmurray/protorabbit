package org.protorabbit.stringtemplate;

import org.protorabbit.model.IContext;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class STGroupDynamic extends StringTemplateGroup {

   IContext ctx = null;
   private String prefix = null;

   public STGroupDynamic( IContext ctx, String prefix) {
       super( prefix );
       this.ctx = ctx;
       this.prefix = prefix;
   }

   private static Logger logger = null;

   static final Logger getLogger() {
       if ( logger == null ) {
           logger = Logger.getLogger( "org.protrabbit" );
       }
       return logger;
   }

   public StringTemplate loadTemplate( String name, StringBuffer buff ) {
       if ( buff == null ) {
           return null;
       }
       StringTemplate template = null;
       try {
           
           if ( buff.toString().trim().startsWith("group")) {
                StringTemplateGroup group = new StringTemplateGroup(new StringReader( buff.toString() ));
                StringTemplate t = group.getInstanceOf( name );
                return t;
           }
           if ( buff != null ) {
               ByteArrayInputStream bis = new ByteArrayInputStream( buff.toString().getBytes() );
               InputStreamReader isr = getInputStreamReader(bis);
               BufferedReader br = new BufferedReader(isr);
               template = loadTemplate(name, br);
               br.close();
               br = null;
           }
       } catch (Exception e) {
           e.printStackTrace();
       }
       return template;
   }
   
   public StringTemplate loadTemplate( String name) {

       StringBuffer buff = null;
       StringTemplate template = null;
       try {
           try {
               buff = ctx.getResource( prefix, name + ".st");
           } catch ( IOException iox) {
               getLogger().log( Level.WARNING, " Could not find template " + name + ".st" );
           }

           if ( buff == null || (buff != null && buff.length() == 0) ) {

               if ( name.endsWith( ".st") ) {
                   name = name.substring(0, name.length() -3 );
               }
               buff = ctx.getVersionedResource(name, "1" );
           }
           if ( buff != null && buff.toString().trim().startsWith("group")) {

                StringTemplateGroup group = new StringTemplateGroup(new StringReader( buff.toString() ));
                StringTemplate t = group.getInstanceOf( name );
                return t;
           }
           if ( buff == null) {
               buff = new StringBuffer( name + " not found. " );
           }
           ByteArrayInputStream bis = new ByteArrayInputStream( buff.toString().getBytes() );
           InputStreamReader isr = getInputStreamReader(bis);
           BufferedReader br = new BufferedReader(isr);
           template = loadTemplate( name, br );
           br.close();
           br = null;
           return template;
       } catch (Exception e) {
           e.printStackTrace();
       }
       return template;
   }

   public synchronized StringTemplate lookupTemplate(StringTemplate enclosingInstance,
           String name) {

        return loadTemplate( name);
  }

}
