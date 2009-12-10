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
import java.io.InputStream;
import java.util.logging.Logger;

import org.protorabbit.accelerator.ICallback;
import org.protorabbit.accelerator.IHttpClient;
import org.protorabbit.accelerator.IWorker;
import org.protorabbit.model.IContext;
import org.protorabbit.util.IOUtil;

public class AsyncWorker implements IWorker, Runnable{

    private ICallback callback = null;
    private String encoding = null;
    private IHttpClient hc = null;
    private String resource;
    private static Logger logger = null;

    static final Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("org.protrabbit");
        }
        return logger;
    }

    public AsyncWorker(String resource, IContext ctx) {
        this.resource = resource;
        hc = ctx.getConfig().getHttpClient(resource);
    }

    public void run(ICallback c, IContext ctx) {
        this.callback = c;

        Thread t=new Thread (this);
        t.start();
    }

    public void run() {
        try {
            StringBuffer buff = null;
            InputStream is = hc.getInputStream();
            if (is != null) {
                encoding = hc.getContentEncoding();
               buff = IOUtil.loadStringFromInputStream(is,encoding);
            } else {
                getLogger().severe("Error loading external resource " + resource);
                buff = new StringBuffer("");
            }
            if (callback != null) {
                callback.setContent(buff);
                callback.execute();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

}
