package org.protorabbit.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;
import org.protorabbit.json.Serialize;

public class ClassUtil {

    private static Logger logger = null;

    static Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("org.protrabbit");
        }
        return logger;
    }
    /* 
     * Find a setter for a param name
     * 
     */
     public static List<Method> getMethods(Object pojo, String param, boolean includeSuper) {
         List<Method> list = new ArrayList<Method>();
         Method[] methods = null;
         if (param.length() > 0) {
             String mName = "set" + (param.substring(0, 1)).toUpperCase();
             if (mName.length() > 1) {
                 mName += param.substring(1, param.length());
             }
             System.out.println("looking for method name=" + mName);
             if (includeSuper) {
                 methods =  pojo.getClass().getMethods();
             } else {
                 methods = pojo.getClass().getDeclaredMethods();
             }
             for (Method m : methods) {
                 if (m.getName().equals(mName)) {
                     Class<?>[] paramTypes = m.getParameterTypes();
                     if (paramTypes.length == 1 ) {
                         list.add(m);
                         System.out.println("adding " + mName);
                     }
                 }
             }
         }
         return list;
     }

    /*
     * Get a 1 argument method matching the name and assignable with a given property
     */

     @SuppressWarnings("unchecked")
    public static boolean mapParam(List<Method> methods, String param, String value, Object targetObject) {

         for (Method m : methods) {
             
             try {
                 Class<?>[] paramTypes = m.getParameterTypes();
                 Object[] args = {1};
 
                 Class<?>  p1 = paramTypes[0];
                 if (p1.isAssignableFrom(Long.class)) {
                     args[0] = new Long(value);
                     System.out.println("*we are a long");
                 } else if (p1.isAssignableFrom(Double.class)) {
                     args[0] = new Double(value);
                 } else if (p1.isAssignableFrom(Integer.class)) {
                     args[0] = new Integer(value);
                 } else if (p1.isAssignableFrom(String.class)) {
                     args[0] = value;
                     System.out.println("we are a string");
                 } else if (p1.isAssignableFrom(Enum.class)) {
                     args[0] = Enum.valueOf((Class<? extends Enum>)p1, value);
                 } else if (p1.isAssignableFrom(Boolean.class)) {
                     args[0] = new Boolean(value);
                 }
                try {
    
                    if (m != null && args[0] != null) {

                        m.invoke(targetObject, args);
                    }
                    System.out.println("Successufly called " + m.getName() + " with value " + value);
                    return true;
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
             } catch (Exception e) {
                 System.out.println("method : " + m + " failed");
             }
        }
        return false;
     }
}
