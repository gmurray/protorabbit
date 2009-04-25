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

public interface IProperty extends IUATestable {
	
    public static final int STRING = 1;
    public static final int INCLUDE = 2;
    
    public String getUATest();
    public void setUATest(String test);
    public String getBaseURI();
    public long getTimeout();
    public void setTimeout(long to);
    public int getType();
    public String getValue();
    public String getKey();
    public void setValue(String value);
    public String originalTemplateId();
    public void setDefer(boolean defer);
    public void setDeferContent(StringBuffer deferContent);
    public boolean getDefer();
    public StringBuffer getDeferContent();
}
