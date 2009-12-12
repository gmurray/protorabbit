package org.protorabbit.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;

import org.protorabbit.accelerator.IHeader;
import org.protorabbit.accelerator.IHttpClient;
import org.protorabbit.accelerator.impl.HttpClient;
import org.protorabbit.util.IOUtil;
import java.util.Random;

public class WebClient extends Thread {

    int runcount = 1;
    long totalRead = 0;
    long timeout = 0;
    String runnerId = null;
    Thread t = null;
    boolean randomRange = false;
    Random rand = null;

    public WebClient( String runnerId, int runcount, long timeout ) {
        this.runcount = runcount;
        this.timeout = timeout;
        this.runnerId = runnerId;

    }

    public void start() {
        super.start();
    }

    public void setRandomRange(boolean rr) {
        if (rr) {
            rand = new Random();
        }
        randomRange = rr;
    }

    public void run() {
        for ( int count=0; count < runcount; count++ ) {

        IHttpClient c = new HttpClient();
        long start = System.currentTimeMillis();
        c.setURL("http://localhost:8080/protorabbit/welcome.prt");
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
        int bytesRead = 0;
        try {
            InputStream in  = c.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            IOUtil.writeBinaryResource( in, bos );
            // show the page
            //System.out.println(bos.toString());
            bytesRead = bos.size();
            totalRead += bytesRead;
            if (bytesRead != 4627) {
             System.out.println(bos);
            }
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
        long st = 0;
        if ( randomRange ) {
            st = rand.nextInt( (int)timeout );
        } else {
            st = timeout;
        }
        System.out.println( runnerId + " run " + count + " elapsed time : " + (stop - start) + "ms" + " bytesRead : " + bytesRead + " total bytes read : " + totalRead + " sleeping for : " + st);
        if (bytesRead != 4627) {
            System.exit(0);
           }
        try {
            sleep( st );
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        }
    }

    
}
