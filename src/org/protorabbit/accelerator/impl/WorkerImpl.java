package org.protorabbit.accelerator.impl;

import java.io.IOException;

import org.protorabbit.accelerator.ICallback;
import org.protorabbit.accelerator.IWorker;
import org.protorabbit.model.IContext;

public class WorkerImpl implements IWorker{

    private IContext ctx = null;
    private String baseDir;
    private String resource;
    private ICallback callback = null;

    public WorkerImpl(String baseDir, 
                      String resource,
                      IContext ctx) {
        this.ctx = ctx;
        this.baseDir = baseDir;
        this.resource = resource;
    }

    public void run(ICallback c) {
        this.callback = c;
        this.run();
        // In the future we canmake the worker also Implement runnable and spin off a thread for non
        // web app resources here using an HTTP client
        //Thread t=new Thread (this);
        //t. start();
    }

    public void run() {
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
