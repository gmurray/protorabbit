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

import org.protorabbit.accelerator.ICallback;
import org.protorabbit.accelerator.IHttpClient;
import org.protorabbit.accelerator.IWorker;
import org.protorabbit.model.IContext;
import org.protorabbit.util.IOUtil;

public class AsyncWorker implements IWorker, Runnable{

    private ICallback callback = null;
    private String encoding = null;
    private IHttpClient hc = null;

    public AsyncWorker(String resource, IContext ctx) {

        hc = ctx.getConfig().getHttpClient(resource);
    }

    public void run(ICallback c) {
        this.callback = c;

        Thread t=new Thread (this);
        t.start();
    }

    public void run() {
        try {

            InputStream is = hc.getInputStream();
            encoding = hc.getContentEncoding();
            StringBuffer buff = IOUtil.loadStringFromInputStream(is,encoding);

            if (callback != null) {
                callback.setContent(buff);
                callback.execute();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

}
