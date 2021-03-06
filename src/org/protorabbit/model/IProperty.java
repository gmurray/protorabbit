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

public interface IProperty extends ITestable {

    public static final int STRING = 1;
    public static final int INCLUDE = 2;

    public String getTest();
    public String getId();
    public String getVersion();
    public void setVersion(String versionId);
    public void setId(String id);
    public void setTest(String test);
    public String getBaseURI();
    public Long getTimeout();
    public void setTimeout(Long to);
    public int getType();
    public String getValue();
    public String getKey();
    public void setValue(String value);
    public String originalTemplateId();
    public void setDefer(Boolean defer);
    public void setDeferContent(StringBuffer deferContent);
    public Boolean getDefer();
    public StringBuffer getDeferContent();
}
