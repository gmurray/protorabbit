package org.protorabbit.stringtemplate;

import org.protorabbit.model.IContext;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;

import java.io.*;

public class STGroupDynamic extends StringTemplateGroup {

   IContext ctx = null;
   private String prefix = null;

   public STGroupDynamic( IContext ctx, String prefix) {
       super( prefix );
       System.out.println("Group loader for String template");
       this.ctx = ctx;
       this.prefix = prefix;
   }

   public StringTemplate loadTemplate( String name, StringBuffer buff ) {
       if ( buff == null ) {
           return null;
       }
       StringTemplate template = null;
       try {
           
           if ( buff.toString().trim().startsWith("group")) {
             System.out.println("we are a group " + name);
            // Use the constructor that accepts a Reader
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
       System.out.println("#Request for prefix " + prefix + " name " + name);
       StringBuffer buff = null;
       StringTemplate template = null;
       try {
           System.out.println("trying for " + prefix + name + ".st" );
           buff = ctx.getResource( prefix, name + ".st");
           System.out.println("buff=====" + buff );
           if ( buff == null || (buff != null && buff.length() == 0) ) {
               System.out.println("trying for " +  name );
               if ( name.endsWith( ".st") ) {
                   name = name.substring(0, name.length() -3 );
               }
               buff = ctx.getVersionedResource(name, "1" );
           }
           if ( buff != null && buff.toString().trim().startsWith("group")) {
             System.out.println("we are a group " + name);
            // Use the constructor that accepts a Reader
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
           template = loadTemplate(name, br);
           br.close();
           br = null;
           return template;
       } catch (Exception e) {
           e.printStackTrace();
       }
       System.out.println("returning null for name " + name );
       return template;
   }

   public synchronized StringTemplate lookupTemplate(StringTemplate enclosingInstance,
           String name) {
       System.out.println("lookup template called for " + name );
        //System.out.println("lookup found "+st.getGroup().getName()+"::"+st.getName());
        return loadTemplate( name);
  }

}
