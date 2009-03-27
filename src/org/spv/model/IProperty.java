package org.spv.model;

public interface IProperty {
	
    public static final int STRING = 1;
    public static final int INCLUDE = 2;
    
    public String getBaseURI();
    public long getTimeout();
    public void setTimeout(long to);
    public int getType();
    public String getValue();
    public String getKey();
    public void setValue(String value);
    public String originalTemplateId();
}
