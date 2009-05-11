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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.protorabbit.Config;

public class JSONUtil {

    /*
     * Clone a JSONObject
     */
    @SuppressWarnings("unchecked")
    public static Object cloneJSON(Object target) {

        if (target == null) return JSONObject.NULL;
        
        if (target instanceof JSONObject) {
            Object o = null;
            o = new JSONObject();
            JSONObject jo = (JSONObject)target;
            Iterator<String> it = jo.keys();
            while (it.hasNext()) {
                String key = it.next();
                try {
                    ((JSONObject)o).put(key,cloneJSON(jo.get(key)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return o;
        } else if (target instanceof JSONArray) {
            Object o = new JSONArray();
            JSONArray ja = (JSONArray)target;
            int len = ja.length();
            for (int i=0; i < len;i++) {
                try {
                    ((JSONArray)o).put(cloneJSON(ja.get(i)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else if (target instanceof Long) {
            return new Long(((Long)target).longValue());
        } else if (target instanceof Double) {
           return new Double(((Double)target).doubleValue());
        } else if (target instanceof Integer) {
           return new Integer(((Integer)target).intValue());
        } else if (target instanceof Boolean) {
           return new Boolean(((Boolean)target).booleanValue());
        }
        return target;
    }
    public static void mixin(JSONObject child, JSONObject parent) {
        String[] blank = {};
        mixin( child,  parent, blank ); 
    }
    
    @SuppressWarnings("unchecked")
    public static void mixin(JSONObject child, JSONObject parent, String[] skip) {
        Iterator<String> it = parent.keys();
        while (it.hasNext()) {
            String key = it.next();
            // don't mix in the skip
            for (int i=0; i < skip.length;i++) {
                if (key.equals(skip[i])) {
                    continue;
                }
            }
            if (child.has(key)) {
                continue;
            } else {
                Object o;
                try {
                    o = parent.get(key);
                    child.put(key, cloneJSON(o));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }      
    }

   public static void concatJSONArrays(JSONArray parent, JSONArray child) throws JSONException {
       for (int i=0; i < child.length(); i++) {
           parent.put(child.get(i));
       }
   }

   public static JSONObject loadFromFile(String fileName) {
        try {
            File f = new File(fileName);
            FileInputStream in = new FileInputStream(f);
            return loadFromInputStream(in);
        } catch (Exception e) {
            Config.getLogger().severe("JSONUtil: error loading JSON from file " + e);
        }
        return null;
    }

   public static void saveToFile(String fileName, JSONObject jo) {
        try {
            File f = new File(fileName);
            FileOutputStream out = new FileOutputStream(f);
            saveToInputStream(new ByteArrayInputStream(jo.toString().getBytes()), out);
        } catch (Exception e) {
            System.out.println("JSONUtil: error writing in json " + e);
        }
    }
   
   public static void saveToInputStream(ByteArrayInputStream in, OutputStream out) {
        try {

            byte[] buffer = new byte[1024];
            int read = 0;
            while (true) {
                read = in.read(buffer);
                if (read <= 0)
                    break;
                out.write(buffer, 0, read);
            }
        } catch (Exception e) {
            System.out.println("JSONUtil: error reading in json " + e);
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.flush();
                    out.close();
                }
            } catch (Exception e) {
            }
        }
    }

    public static JSONObject loadFromInputStream(InputStream in) {
        if (in == null) {
            return null;
        }
        ByteArrayOutputStream out = null;
        try {

            byte[] buffer = new byte[1024];
            int read = 0;
            out = new ByteArrayOutputStream();
            while (true) {
                read = in.read(buffer);
                if (read <= 0)
                    break;
                out.write(buffer, 0, read);
            }
            return new JSONObject(out.toString());
        } catch (Exception e) {
            Config.getLogger().severe("JSONUtil: error reading in JSON from stream : " + e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.flush();
                    out.close();
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

}
