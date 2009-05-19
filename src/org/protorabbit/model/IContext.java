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

import java.io.IOException;
import java.util.Map;

import org.protorabbit.Config;

public interface IContext {

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
}
