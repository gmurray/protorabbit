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

import org.protorabbit.accelerator.CacheContext;
import org.protorabbit.accelerator.ICallback;
import org.protorabbit.accelerator.IWorker;
import org.protorabbit.model.IContext;


public class DeferredResource extends CacheableResource {

    public static final int LOADING = 100;
    public static final int LOADED = 200;

    private int status;
    private IWorker worker;
    private ICallback callback;

    public DeferredResource(String baseDir, String resource, IContext ctx, Long timeout) {
        super();
        cc = new CacheContext(timeout, hash);
        callback = new DeferredCallback(this);
        if (resource.startsWith("http")) {
            worker = new AsyncWorker(resource, ctx);
            // start immediately
            this.refresh(ctx);
        } else {
            worker = new Worker(baseDir,resource,ctx);
        }
    }

    public void refresh(IContext ctx) {

        if (status != DeferredResource.LOADING &&
            status != DeferredResource.LOADED) {
            status = DeferredResource.LOADING;
            worker.run(callback);
        }
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
