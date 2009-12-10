package org.protorabbit.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.protorabbit.accelerator.IHttpClient;
import org.protorabbit.accelerator.impl.HttpClient;
import org.protorabbit.accelerator.IHeader;
import org.protorabbit.util.IOUtil;

public class Engine {

    public static void main(String[] args) {
        System.out.println("Engine start!");
        IHttpClient c = new HttpClient();
        c.setURL("http://google.com/search?q=greg+murray");
        /*
        Map params = new HashMap<String, String>();
        params.put("q", "Greg Murray");
        try {
            c.doPost(params, null);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        */

        try {
            InputStream in  = c.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            IOUtil.writeBinaryResource( in, bos );
            System.out.println(bos.toString());
            System.out.println("Page size : " + bos.size() + " bytes.");
            System.out.println("Getting headers...");
            // get the headers
            List<IHeader> rh = c.getResponseHeaders();
            int hcount = 0;
            if (rh != null) {
                hcount = rh.size();
            }
            System.out.println("Header count is " + hcount);
            for (IHeader e : rh ) {
               System.out.println(e.getKey() + ":" + e.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
