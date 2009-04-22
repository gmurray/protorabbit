package org.protorabbit.accelerator.impl;

import org.protorabbit.accelerator.ICallback;

public class DeferredCallback implements ICallback {

    private DeferredResource deferredResource;
    private StringBuffer content;
    private String contentType = "text/html";
    private int status = -1;

    public DeferredCallback(DeferredResource dr) {
        this.deferredResource = dr;
    }

    public void execute() {
        deferredResource.reset();
        deferredResource.setContent(content);
        deferredResource.setLoaded(true);
        deferredResource.setStatus(200);
    }

    public StringBuffer getContent() {
        return content;
    }

    public String getContentType() {
        return contentType;
    }

    public int getStatus() {
        return status;
    }

    public void setContent(StringBuffer content) {
        this.content = content;
    }

    public void setStatus(int status) {
        this.status = status;
    }

}
