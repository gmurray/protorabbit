package org.spv.model.impl;

import org.spv.model.IProperty;

public class PropertyImpl implements IProperty {
	
	private String baseURI = null;
	private String value = null;
	private String key = null;
	private int type = -1;
	private String originalTemplate = null;
	private long timeout = 0;

	public PropertyImpl(String key, String value, int type, String baseURI, String originalTemplate) {
		this.key = key;
		this.value = value;
		this.type = type;
		this.baseURI = baseURI;
		this.originalTemplate = originalTemplate;
	}
	
	public String getBaseURI() {
		return baseURI;
	}

	public String getKey() {
		return key;
	}
	
	public String getValue() {
		return value;
	}

	public int getType() {
		return type;
	}

	public String originalTemplateId() {
		return originalTemplate;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
	
}
