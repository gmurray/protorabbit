package org.protorabbit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.zip.GZIPOutputStream;

public class IOUtil {
	
    private static SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss  z");
	
    public static byte[] getGZippedContent(byte[] content) {
    	byte[] gzippedContent = null;
    	if (content != null) {
    		ByteArrayOutputStream bos = new ByteArrayOutputStream();
    		ByteArrayInputStream bis =
    		   new ByteArrayInputStream(content);
    	    try {
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
		    } catch (java.io.IOException ioe) {
		    	System.err.println("IOUtil exception : " + ioe);
		    	ioe.printStackTrace();
		    }
    	}
    	return gzippedContent;	   
    }
   
    public static void writeBinaryResource(InputStream in, OutputStream out) {
	    byte[] bytes = new byte[1024];
	    try {
	      int read = 0;
	      while ((read = in.read(bytes)) > 0) {
	           out.write(bytes, 0, read);
	      }
	      in.close();
	      out.close();
	    } catch (IOException e) {
	        System.out.println("IOUtil stream error : "  + e);
	    }
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
}
