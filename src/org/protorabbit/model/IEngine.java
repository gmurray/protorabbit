package org.protorabbit.model;

import java.io.OutputStream;

public interface IEngine {

    public static final String DEFAULT_COMMAND_LIST = "protorabbit.IEngine.DEFAULT_COMMAND_LIST";
    public static final String LAST_COMMAND_LIST = "protorabbit.IEngine.LAST_COMMAND_LIST";
    public static final String BUFFERS = "protorabbit.IEngine.BUFFERS";

    /*
     * Render a template with the id tid, context, and given outputsteam
     */
    public void renderTemplate(String tid, IContext ctx,  OutputStream out);

}