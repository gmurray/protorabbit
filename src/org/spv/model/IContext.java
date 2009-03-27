package org.spv.model;

import java.io.IOException;

import org.spv.Config;

public interface IContext {
	
    public Config getConfig();
    
    public String getTemplateId();
    
    public void setTemplateId(String templateId);
    
    public StringBuffer getResource(String baseDir, String name) throws IOException;
    
    public boolean resourceExists(String name);

	public String getContextRoot();
	
	 public boolean isUpdated(String name, long lastUpdate);
}
