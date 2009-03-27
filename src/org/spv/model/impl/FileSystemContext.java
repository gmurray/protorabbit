package org.spv.model.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.spv.Config;
import org.spv.json.JSONUtil;
import org.spv.model.IContext;

public class FileSystemContext implements IContext {
	
	Config cfg;
	private String templateId;
	private String contextRoot = "";
	
    public FileSystemContext(Config cfg, String contextRoot) {
    	this.cfg = cfg;
		this.contextRoot = contextRoot;
	}

	public Config getConfig() {
    	return cfg;
    }
    
    public String getTemplateId() {
    	return templateId;
    }
    
    public StringBuffer getResource(String baseDir, String name) throws IOException {
        // / signifies the base directory
    	if (name.startsWith("/")) {
    		name = name.substring(1);
    		baseDir = contextRoot;
    	}
    	String contents = JSONUtil.loadStringFromInputStream(new FileInputStream(baseDir + name));
    	if (contents != null) {
    		return new StringBuffer(contents);
    	}
    	return null;
    }

	public void setTemplateId(String templateId) {
		this.templateId = templateId;		
	}

	public boolean resourceExists(String name) {
		File f = new File(name);
		return f.exists();
	}

	public String getContextRoot() {
		return contextRoot;
	}

	public boolean isUpdated(String name, long lastUpdate) {
		File f = new File(name);
		long lastModified = f.lastModified();
		return (lastUpdate > lastModified);
	}

}
