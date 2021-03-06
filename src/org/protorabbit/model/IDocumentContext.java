package org.protorabbit.model;

import java.util.List;

import org.protorabbit.model.impl.ResourceURI;

public interface IDocumentContext {

    public int getIndex();
    public void setIndex(int index);
    public StringBuffer getDocument();
    public long getContentLength();
    public long getCreated();
    public void setRequiresRefresh(boolean requiresRefresh);
    public long getLastAccessed();
    public long getLastRefresh();
    public void setDocument(StringBuffer document);

    public List<ICommand> getAllCommands();
    public List<ICommand> getBeforeCommands();
    public List<ICommand> getDefaultCommands();
    public List<ICommand> getAfterCommands();

    public void setAllCommands(List<ICommand> cmds);
    public void setBeforeCommands(List<ICommand> cmds);
    public void setDefaultCommands(List<ICommand> cmds);
    public void setAfterCommands(List<ICommand> cmds);
    public void setURI(ResourceURI uri);
    public ResourceURI getURI();

}
