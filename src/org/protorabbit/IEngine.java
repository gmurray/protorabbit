package org.protorabbit;

import java.io.OutputStream;

import org.protorabbit.model.IContext;

public interface IEngine {

    public void renderTemplate(String tid, Config cfg, OutputStream out, IContext ctx);

}