package org.protorabbit.model.impl;

import java.util.List;

import org.protorabbit.model.ICommand;
import org.protorabbit.model.IDocumentContext;

public class DocumentContext implements IDocumentContext {

    private StringBuffer document;
    private List<ICommand> allCommands = null;
    private List<ICommand> beforeCommands = null;
    private List<ICommand> defaultCommands = null;
    private List<ICommand> afterCommands = null;

    public int index = 0;
 
    public void setDocument(StringBuffer document) {
        this.document = document;
    }
    public StringBuffer getDocument() {
        return document;
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

}
