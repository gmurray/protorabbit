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

package org.protorabbit.json;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.protorabbit.json.Serialize;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DefaultSerializer implements JSONSerializer {

    private static Logger logger = null;

    static Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("org.protrabbit");
        }
        return logger;
    }

   @SuppressWarnings("unchecked")
   public Object serialize(Object o) {

       // null is null
       if (o == null) {
           return JSONObject.NULL;
       }

       // collections
       if (Collection.class.isAssignableFrom(o.getClass())) {
           Iterator<?> it =  ((Collection<?>)o).iterator();

           JSONArray ja = new JSONArray();
           while(it.hasNext()) {

               Object i = serialize(it.next());
               ja.put(i);
           }
           return ja;
       }

       // maps
       if (Map.class.isAssignableFrom(o.getClass())) {
           JSONObject jo = new JSONObject();
           Map<?, ?> m = ((Map<?, ?>)o);
           Iterator<?> ki =  m.keySet().iterator();
           while (ki.hasNext()) {
               Object key = ki.next();
               Object value = serialize(m.get(key));
               try {
                   jo.put(key.toString(), value);
               } catch (JSONException e) {
                   e.printStackTrace();
               }
           }
           return jo;
       }

       // primitives
       if (o instanceof Double ||
           o instanceof Number ||
           o instanceof Integer ||
           o instanceof String ||
           o instanceof Enum ||
           o instanceof Boolean) {
           return o;
       }

       if (o instanceof Date) {
           return ((Date)o).getTime();
       }

       // convert arrays to collections
       boolean b = o.getClass().isArray();

       if (b) {
           try {
               Object[] objs = (Object[])o;
               List<Object> l = Arrays.asList(objs);
               return serialize(l);
           } catch (ClassCastException e) {
               return JSONObject.NULL;
           }
       }
       // serialize using bean like methods
       return serializePOJO(o);

   }

   /*
    * Look at all the public methods in the object
    * find the ones that start with "get"
    *
    * create a property key for the methods and invoke the method using reflection
    * to get value.
    */
   public Object serializePOJO(Object pojo) {

       Object[] args = {};
       HashMap<String, Object> map = new HashMap<String, Object>();
       Method[] methods = pojo.getClass().getMethods();
       for (int i=0; i < methods.length;i++) {

           Method m = methods[i];
           try {

               // skip if there is a skip annotation
               if (Modifier.isPublic(m.getModifiers()) &&
                    !"getClass".equals(m.getName()) &&
                    !"getParent".equals(m.getName()) &&
                    !"getSystemClassLoader".equals(m.getName()) &&
                    !"getMethods".equals(m.getName()) &&
                    !"getDeclaredClasses".equals(m.getName()) &&
                    !"getConstructors".equals(m.getName()) &&
                    !"getDeclaringClass".equals(m.getName()) &&
                    !"getEnclosingClass".equals(m.getName()) &&
                    !"getClassLoader".equals(m.getName()) &&
                    (m.getName().startsWith("get") ||
                     m.getName().startsWith("is") ) &&
                     m.getName().length() > 2 &&
                    m.getParameterTypes().length == 0) {

                   if (m.isAnnotationPresent(Serialize.class)) {
                       Serialize s = m.getAnnotation(Serialize.class);

                               if ("skip".equals(s.value())) {
                                   continue;
                               }
                           }
                       // change the case of the property from camelCase
                       String key = "";
                       if (m.getName().startsWith("is") &&
                           m.getName().length() > 3) {
                            key += m.getName().substring(2,3).toLowerCase();
                            // get the rest of the name;
                            key += m.getName().substring(3);
                       } else if (m.getName().startsWith("get") &&
                                  m.getName().length() > 4) {
                           key +=  m.getName().substring(3,4).toLowerCase();
                           // get the rest of the name;
                           key += m.getName().substring(4);
                       } 
                       Object value =  m.invoke(pojo, args);
                       map.put(key, value);
               }
           } catch (IllegalArgumentException e) {
               getLogger().warning("Unable to serialize " + pojo + " : " + e);
           } catch (IllegalAccessException e) {
               getLogger().warning("Unable to serialize " + pojo + " : " + e);
           } catch (InvocationTargetException e) {
               getLogger().warning("Unable to serialize " + pojo + " : " + e);
           }
       }
       // use the serializer itself to serialize a map of properties we created
       if (map.keySet().size() > 0) {
           return serialize(map);
       }

       return JSONObject.NULL;
   }

   /*
    * Get a 1 argument method matching the name and assignable with a given property
    */
    @SuppressWarnings("unchecked")
    void invokeMethod(Method[] methods, String key, String name, JSONObject jo, Object targetObject) {
        Object param = null;
        for (int i=0;i < methods.length; i++) {
            Method m = methods[i];
            if (m.getName().equals(name)) {
                Class<?>[] paramTypes = m.getParameterTypes();
                if (paramTypes.length == 1 && jo.has(key)) {
                     Class<?> tparam =  paramTypes[0];
                     try {
                         if (Long.class.isAssignableFrom(tparam)) {
                             param = new Long(jo.getLong(key));
                         } else if (Double.class.isAssignableFrom(tparam)) {
                             param = new Double(jo.getDouble(key));
                         } else if (Integer.class.isAssignableFrom(tparam)) {
                             param = new Integer(jo.getInt(key));
                         } else if (String.class.isAssignableFrom(tparam)) {
                             param = jo.getString(key);
                         } else if (Enum.class.isAssignableFrom(tparam)) {
                             param = Enum.valueOf((Class<? extends Enum>)tparam, jo.getString(key));
                         } else if (Boolean.class.isAssignableFrom(tparam)) {
                             param = new Boolean(jo.getBoolean(key));
                         } else if (jo.isNull(key)) {
                             param = null;
                         }
                    } catch (JSONException e) {
                            e.printStackTrace();
                    }

                      if (param != null) {

                          try {

                              if (m != null) {
                                  Object[] args = {param};
                                  m.invoke(targetObject, args);

                              }
                          } catch (SecurityException e) {
                              e.printStackTrace();
                          } catch (IllegalArgumentException e) {
                              e.printStackTrace();
                          } catch (IllegalAccessException e) {
                              e.printStackTrace();
                          } catch (InvocationTargetException e) {
                              e.printStackTrace();
                          }
                    }
                }
            }
        }

    }

    public void deSerialize(String jsonText, Object targetObject) {
        try {
            JSONObject jo = new JSONObject(jsonText);
            deSerialize(jo, targetObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
    }

    @SuppressWarnings("unchecked")
    public void deSerialize(Object jsonObject, Object targetObject) {

        if (jsonObject != null) {
            JSONObject jo = (JSONObject)jsonObject;
            Method[] methods = targetObject.getClass().getMethods();
            Iterator<String> it = jo.keys();
            while (it.hasNext()) {
                String key = it.next();
                // jump out if the key length is too short
                if (key.length() <= 1) continue;
                String mName = "set" + key.substring(0,1).toUpperCase() + key.substring(1);
                invokeMethod( methods,key,  mName, jo, targetObject);
            }
        }
    }

    public Object deSerialize(String jsonText, Class<?> targetClass) {
        try {
            Object o = targetClass.newInstance();
            deSerialize(jsonText, o);
            return o;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
     /*
        DefaultSerializer df = new DefaultSerializer();
        TestObject to = new TestObject();
        to.setFirstName("Greg");
        to.setLastName("Murray");
        to.setTimeout(55L);
        to.setFoo( TestObject.Foo.One);
        System.out.println("Original Object=" + to);
        JSONObject json = (JSONObject) df.serialize(to);
        System.out.println("JSON Object=" + json);
        TestObject to2 = (TestObject) df.deSerialize(json.toString(), TestObject.class);
        System.out.println("After=" + to2);
        */
    }
}