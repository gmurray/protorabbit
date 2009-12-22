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

package org.protorabbit.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.protorabbit.Config;

public interface IContext {

    public ITemplate getTemplate();
    public void setTemplate( ITemplate template );

    public List<String> getUAScriptTests();

    public List<String> getUAStyleTests();

    public void addUAScriptTest(String test);

    public void addUAStyleTest(String test);

    public Map<String, ?> getAttributes();

    public Object getAttribute(String key);

    public void setAttribute(String key, Object value);

    public Config getConfig();

    public String getTemplateId();

    public void setTemplateId(String templateId);

    public StringBuffer getResource(String baseDir, String name) throws IOException;

    public Object parseExpression(String expression); 

    public boolean resourceExists(String name);

    public String getContextRoot();

    public boolean isUpdated(String name, long lastUpdate);

    public long getLastUpdated(String name);

    public boolean uaTest(String test);

    public boolean test(String test);

    public void destroy();
    
    public ByteArrayOutputStream getBuffer(String bid);

    public void setBuffer(String uuId, ByteArrayOutputStream bos);
}
