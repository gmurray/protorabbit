package org.protorabbit.accelerator.impl;

import java.io.IOException;
import java.io.InputStream;

import org.protorabbit.IOUtil;
import org.protorabbit.accelerator.ICallback;
import org.protorabbit.accelerator.IWorker;

public class AsyncWorkerImpl implements IWorker, Runnable{

    private String resource;
    private ICallback callback = null;
    private String encoding = null;

    public AsyncWorkerImpl(String resource) {

        this.resource = resource;
    }

    public void run(ICallback c) {
        this.callback = c;

        Thread t=new Thread (this);
        t.start();
    }

    public void run() {
        try {

            HttpClient hc = new HttpClient(resource);
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
