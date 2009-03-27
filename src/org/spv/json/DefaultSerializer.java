package org.spv.json;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DefaultSerializer implements JSONSerializer {

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
			Map m = ((Map<String, ?>)o);
			Iterator<String> ki =  m.keySet().iterator();
			while (ki.hasNext()) {
				String key = ki.next();
				Object value = serialize(m.get(key));
				try {
					jo.put(key, value);
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
			o instanceof Boolean) {
			return o;
		}
		
		if (o instanceof Date) {
			return ((Date)o).getTime();
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
		
		Method[] methods = pojo.getClass().getDeclaredMethods();
		
		for (int i=0; i < methods.length;i++) {
			try {
				Method m = methods[i];			
				if (Modifier.isPublic(m.getModifiers()) &&
				    m.getName().startsWith("get") && m.getName().length() > 3) {


				
					// change the case of the property from camelCase
					String key = m.getName().substring(3,4).toLowerCase();
					// get the rest of the name;
					if (m.getName().length() > 4) {
						key += m.getName().substring(4);
					}
					Object value =  m.invoke(pojo, args);			
					map.put(key, value);
				}		
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		// use the serializer itself to serialize a map of properties we created
		if (map.keySet().size() > 0) {
			return serialize(map);
		}
		
		return JSONObject.NULL;
	}
	

}
