package org.protorabbit.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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

                 Class<?>[] paramTypes = m.getParameterTypes();
                 Object[] args = new Object[1];
 
                 Class<?>  p1 = paramTypes[0];

                 if (Long.class.isAssignableFrom(p1) || p1 == long.class && !"".equals(value) ) {
                     try {
                         args[0] =  new Long(Long.parseLong(value));
                     } catch ( NumberFormatException nfe ) {
                         throw new RuntimeException("Number format error converting value " + value + " to a long / Long on method " + m.getName() );
                     }
                 } else if (Double.class.isAssignableFrom(p1) || p1 == double.class && !"".equals(value) ) {
                     try {
                         args[0] = new Double(Double.parseDouble(value));
                     } catch ( NumberFormatException nfe ) {
                         throw new RuntimeException("Number format error converting value " + value + " to a double / Double on method " + m.getName() );
                     }
                 } else if (Integer.class.isAssignableFrom(p1) || p1 == int.class && !"".equals(value) ) {
                     try {
                         args[0] = new Integer(Integer.parseInt(value));
                     } catch ( NumberFormatException nfe ) {
                         throw new RuntimeException("Number format error converting value " + value + " to a int / Integer on method " + m.getName() );
                     }
                 } else if (Enum.class.isAssignableFrom(p1)) {
                     args[0] = Enum.valueOf((Class<? extends Enum>)p1, value);
                 } else if (String.class.isAssignableFrom(p1) || p1 == String.class) {
                     args[0] = value;
                 } else if (Boolean.class.isAssignableFrom(p1) || p1 == boolean.class) {
                     args[0] =  new Boolean(Boolean.parseBoolean(value));
                 } else {
                     throw new RuntimeException("Unsupported argument type : " + p1 + " with value " + value );
                }
                try {
                    if (m != null && args[0] != null) {
                        m.invoke(targetObject, args);
                    }
                    return true;
                } catch (SecurityException e) {
                    throw new RuntimeException("SecurityException : " + p1 + " with value " + value );
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("IllegalArgumentException with argument type : " + p1 + " with value " + value );
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("IllegalAccessException with argument type : " + p1 + " with value " + value );
                } catch (InvocationTargetException e) {
                    throw new RuntimeException("InvocationTargetException with argument type : " + p1 + " with value " + value );
                }

        }
        return false;
     }
}
