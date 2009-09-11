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

public interface JSONSerializer {
    public Object serialize(Object o);
    public Object genericDeserialize(String jsonText);
    public void deSerialize(String jsonObject, Object targetObjects);
    public Object deSerialize(String jsonText, Class<?> targetClass);
}
