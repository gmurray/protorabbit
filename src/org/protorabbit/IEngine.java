package org.protorabbit;

import java.io.OutputStream;
import java.util.List;

import org.protorabbit.model.ICommand;
import org.protorabbit.model.IContext;

public interface IEngine {

    public static final String DEFAULT_COMMAND_LIST = "protorabbit.IEngine.DEFAULT_COMMAND_LIST";
    public static final String LAST_COMMAND_LIST = "protorabbit.IEngine.LAST_COMMAND_LIST";
    public static final String BUFFERS = "protorabbit.IEngine.BUFFERS";

    public void renderTemplate(String tid, IContext ctx,  OutputStream out);
    public List<ICommand> getCommands(Config cfg, StringBuffer doc);

}