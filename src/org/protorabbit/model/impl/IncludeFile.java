package org.protorabbit.model.impl;

import java.util.Date;

import org.protorabbit.model.IContext;

public class IncludeFile {
	
	private String uri;
	private StringBuffer content;
	private long timeout;
	private long lastRefresh;
	
	public IncludeFile(String uri,
			           StringBuffer content) {
		this.content = content;
		this.uri = uri;
		lastRefresh = (new Date()).getTime();
	}
	
	public String getURI() {
		return uri;
	}

	public StringBuffer getContent() {
		return content;
	}

	public void setContent(StringBuffer content) {
		this.content = content;
		// reset fresh counter
		lastRefresh = (new Date()).getTime();
	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public long getLastRefresh() {
		return lastRefresh;
	}
	
	public boolean isStale(IContext ctx) {
		if (ctx.getConfig().getDevMode()) {
		    boolean isUpdated = ctx.isUpdated( uri, lastRefresh);
		    if (isUpdated) return true;
		}
		long now = (new Date()).getTime();
		return (now - lastRefresh > timeout);
	}

	public void setLastRefresh(long lastRefresh) {
		this.lastRefresh = lastRefresh;
	}


}
