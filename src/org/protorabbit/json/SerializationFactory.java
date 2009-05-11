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

public class SerializationFactory {

    public static String DEFAULT_SERIALIZER = "org.protorabbit.json.DefaultSerializer";
    private String factoryClass = DEFAULT_SERIALIZER;

    private JSONSerializer serializer = null;

    public SerializationFactory() {
    }

    public SerializationFactory(String factoryClass) {
        this.factoryClass = factoryClass;
    }

    public JSONSerializer getInstance() {
        if (serializer == null) {
            try {
                Object o = Class.forName(factoryClass).newInstance();
                if (o != null) {
                    serializer = (JSONSerializer)o;
                }
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return serializer;
    }
}
