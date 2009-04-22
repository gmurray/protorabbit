package org.protorabbit.accelerator.impl;

import org.protorabbit.accelerator.CacheContext;
import org.protorabbit.model.IContext;


public class DeferredResource extends CacheableResource {

    private int status;
    private String baseDir;
    private String resource;

    public DeferredResource(String baseDir, String resource, IContext ctx, long timeout) {
        super();
        cc = new CacheContext(timeout, hash);
        this.baseDir = baseDir;
        this.resource = resource;
        refresh(ctx);
    }
    
    public void refresh(IContext ctx) {
        DeferredCallback dc = new DeferredCallback(this);
        WorkerImpl worker = new WorkerImpl(baseDir,resource,ctx);
        worker.run(dc);
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
