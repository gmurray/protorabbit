package org.protorabbit.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.protorabbit.accelerator.IHeader;
import org.protorabbit.accelerator.IHttpClient;
import org.protorabbit.accelerator.impl.HttpClient;
import org.protorabbit.util.IOUtil;

public class WebClient implements Runnable {

    int runcount = 1;
    int count = 0;
    long totalRead = 0;
    long timeout = 0;
    String runnerId = null;
    Thread t = null;

    public WebClient( String runnerId, int runcount, long timeout ) {
        this.runcount = runcount;
        this.timeout = timeout;
        this.runnerId = runnerId;
        t=new Thread (this);
        t.start();
    }

    @Override
    public void run() {
        if ( count >= runcount ) {
            return;
        }

        IHttpClient c = new HttpClient();
        long start = System.currentTimeMillis();
        c.setURL("http://localhost:8080/protorabbit/");
        /*
        Map params = new HashMap<String, String>();
        params.put("q", "java");
        try {
            c.doPost(params, null);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        */
        int hcount = 0;
        try {
            InputStream in  = c.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            IOUtil.writeBinaryResource( in, bos );
            // show the page
            //System.out.println(bos.toString());
            totalRead += bos.size();
            //System.out.println("Getting headers...");
            // get the headers
            List<IHeader> rh = c.getResponseHeaders();

            if (rh != null) {
                hcount = rh.size();
            }
            // list out the headers
            //for (IHeader e : rh ) {
            //   System.out.println(e.getKey() + ":" + e.getValue());
            // }
        } catch (IOException e) {
            e.printStackTrace();
        }
        long stop = System.currentTimeMillis();
        System.out.println( runnerId + " run " + count + " elapsed time : " + (stop - start) + "ms" + " header count : " + hcount + " bytes read : " + totalRead );

        try {
            t.sleep( timeout );
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        count++;
        run();
    }

    
}
