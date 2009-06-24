package org.protorabbit.model.impl;

import java.util.List;
import java.util.Date;

import org.protorabbit.model.ICommand;
import org.protorabbit.model.IDocumentContext;

public class DocumentContext implements IDocumentContext {

    private StringBuffer document;
    private List<ICommand> allCommands = null;
    private List<ICommand> beforeCommands = null;
    private List<ICommand> defaultCommands = null;
    private List<ICommand> afterCommands = null;
    private long created;
    private long lastAccessed;
    private long lastRefresh;
    private ResourceURI uri = null;
    private boolean requiresRefresh = false;

    public int index = 0;
 
    public DocumentContext() {
        Date now = new Date();
        created = lastRefresh = now.getTime();
    }

    public void setDocument(StringBuffer document) {
        Date now = new Date();
        lastRefresh = now.getTime();
        this.document = document;
    }

    public StringBuffer getDocument() {
        Date now = new Date();
        lastAccessed = now.getTime();
        return document;
    }
    
    public long getContentLength() {
        if (document == null) {
            return 0;
        } else {
            return document.toString().getBytes().length;
        }
    }

    public long getCreated() {
        return created;
    }

    public long getLastRefresh() {
        return lastRefresh;
    }

    public long getLastAccessed() {
        return lastAccessed;
    }

    public void setDefaultCommands(List<ICommand> defaultCommands) {
        this.defaultCommands = defaultCommands;
    }
    public List<ICommand> getDefaultCommands() {
        return defaultCommands;
    }

    public void setBeforeCommands(List<ICommand> beforeCommands) {
        this.beforeCommands = beforeCommands;
    }

    public List<ICommand> getBeforeCommands() {
        return beforeCommands;
    }

    public void setAfterCommands(List<ICommand> afterCommands) {
        this.afterCommands = afterCommands;
    }

    public List<ICommand> getAfterCommands() {
        return afterCommands;
    }

    public List<ICommand> getAllCommands() {
        return allCommands;
    }

    public void setAllCommands(List<ICommand> allCommands) {
        this.allCommands = allCommands;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public void setURI(ResourceURI uri) {
        this.uri = uri;
    }

    public ResourceURI getURI() {
        return uri;
    }

    public void setRequiresRefresh(boolean requiresRefresh) {
        this.requiresRefresh = requiresRefresh;
    }
    
    public boolean requiresRefresh() {
        return requiresRefresh;
    }

}
