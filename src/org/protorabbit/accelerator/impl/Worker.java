/*
 * Protorabbit
 *
 * Copyright (c) 2009 Greg Murray (protorabbit.org)
 * 
 * Licensed under the MIT License:
 * 
 *  http://www.opensource.org/licenses/mit-license.php
 *
 */

package org.protorabbit.accelerator.impl;

import java.io.IOException;

import org.protorabbit.accelerator.ICallback;
import org.protorabbit.accelerator.IWorker;
import org.protorabbit.model.IContext;

public class Worker implements IWorker{

    private String baseDir;
    private String resource;
    private ICallback callback = null;

    public Worker(String baseDir, 
                      String resource) {

        this.baseDir = baseDir;
        this.resource = resource;
    }

    public void run(ICallback c, IContext ctx) {
        this.callback = c;
        this.run(ctx);
    }

    public void run(IContext ctx) {
        try {

            StringBuffer content = ctx.getResource(baseDir,resource);
            if (callback != null) {
                callback.setContent(content);
                callback.execute();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

}
