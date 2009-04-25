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

package org.protorabbit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

public class IOUtil {

    private static SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss  z");

    public static byte[] getGZippedContent(byte[] content) throws IOException{
        byte[] gzippedContent = null;
        if (content != null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ByteArrayInputStream bis =
               new ByteArrayInputStream(content);
                GZIPOutputStream out = new GZIPOutputStream(bos);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = bis.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            bis.close();
            out.finish();
            out.close();
            gzippedContent = bos.toByteArray();
        }
        return gzippedContent;
    }

    public static void writeBinaryResource(InputStream in, OutputStream out) throws IOException {
        byte[] bytes = new byte[1024];

          int read = 0;
          while ((read = in.read(bytes)) > 0) {
               out.write(bytes, 0, read);
          }
          in.close();
          out.close();
    }
    
    public static String generateHash(String target) {
        if (target != null) {
            String hash = null;
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] bytes = md.digest(target.getBytes());
                // buffer to write the md5 hash to
                StringBuffer buff = new StringBuffer();
                for (int l = 0; l < bytes.length; l++) {
                    String hx = Integer.toHexString(0xFF & bytes[l]);
                    // make sure the hex string is correct if 1 character
                    if (hx.length() == 1) {
                        buff.append("0");
                    }
                    buff.append(hx);
                }
                hash = buff.toString().trim();
            } catch (java.security.NoSuchAlgorithmException e) {
            }
            return hash;
        }
        return null;
    }

    public static long getMaxAge(long maxAge) {
        long current = System.currentTimeMillis();
        long diff = current  + (maxAge * 1000) ;
        // convert diff to seconds
        return (diff / 1000);
    }

    /*
     *  Get the expires calculated from now
     */
    public static String getExpires(long maxAge) {
        long created = System.currentTimeMillis();
        Date expiredate = new Date(created + (maxAge * 1000) );
        sdf.setTimeZone (TimeZone.getTimeZone("GMT"));
        return sdf.format(expiredate);  
    }

    /*
     *  Load a classpath based resource.
     */
    public static StringBuffer getClasspathResource(Config cfg, String name) throws IOException {
        InputStream is  = cfg.getClass().getResourceAsStream(name);
        StringBuffer resource = null;
        if (is != null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            writeBinaryResource(is,bos);
            try {
                resource = new StringBuffer(bos.toString(cfg.getEncoding()));
            } catch (UnsupportedEncodingException e) {
                Config.getLogger().log(Level.SEVERE, "Error loading Resoruce", e);
            }
        }
        return resource;
    }

    public static StringBuffer loadStringFromInputStream(InputStream in, String encoding) throws IOException {
        ByteArrayOutputStream out = null;
        try {

            byte[] buffer = new byte[1024];
            int read = 0;
            out = new ByteArrayOutputStream();
            while (true) {
                read = in.read(buffer);
                if (read <= 0)
                    break;
                out.write(buffer, 0, read);
            }
            StringBuffer buff = null;
            if (encoding == null) {
                buff = new StringBuffer(out.toString());
            } else {
               buff = new StringBuffer(out.toString(encoding));
            }
            return buff;
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }
}
