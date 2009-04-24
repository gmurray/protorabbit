package org.protorabbit.accelerator.impl;

import java.util.Map;
import java.util.Iterator;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.logging.*;
import java.security.Security;

import org.protorabbit.Config;

   /**
    *
    * HTTPClient supporting both http and https and 
    *  with the ability to set pass in headers and set
    *  as a proxy port / host
    */
   public class HttpClient {

       private String proxyHost = null;
       private int proxyPort = -1;
       private boolean isHttps = false;
       private boolean isProxy = false;
       private HttpURLConnection urlConnection = null;
       private Map<String,String> headers;

       public HttpClient(String url)
               throws MalformedURLException {

           this(null,-1, url, null,"GET");
       }

       /**
        * @param phost PROXY host name
        * @param pport PROXY port string
        * @param url URL string
        * @param headers Map
        */
       public HttpClient(String phost,
                        int pport,
                        String url,
                        Map<String,String> headers,
                        String method)
           throws MalformedURLException {

           if (phost != null && pport != -1) {
               this.isProxy = true;
           }
           this.proxyHost = phost;
           this.proxyPort = pport;
           if (url.trim().startsWith("https:")) {
               isHttps = true;
           }
           this.urlConnection = getURLConnection(url);
           try {
               this.urlConnection.setRequestMethod(method);
           } catch (java.net.ProtocolException pe) {
               Config.getLogger().log(Level.SEVERE, "Unable protocol method to " + method, pe);
           }
           this.headers = headers;
           // seat headers
           if (headers != null) {
               Iterator<String> it = headers.keySet().iterator();
               if (it != null) {
                   while (it.hasNext()) {
                       String key = it.next();
                       String value = headers.get(key);
                       this.urlConnection.setRequestProperty (key, value);
                   }
               }
           }
       }

       /**
        * @param phost PROXY host name
        * @param pport PROXY port string
        * @param url URL string
        * @param headers Map     
        * @param userName string
        * @param password string
        */
       public HttpClient(String phost,
                         int pport,
                         String url,
                         Map<String, String> headers,
                         String method,
                         String userName,
                         String password)
           throws MalformedURLException {

           try {
               if (phost != null && pport != -1) {
                   this.isProxy = true;
               }
               this.proxyHost = phost;
               this.proxyPort = pport;
               if (url.trim().startsWith("https:")) {
                   isHttps = true;
               }
               this.urlConnection = getURLConnection(url);
               try {
                   this.urlConnection.setRequestMethod(method);
               } catch (java.net.ProtocolException pe) {
                   Config.getLogger().severe("Unable protocol method to " + method + " : " + pe);
               }
               // Send HTTP Basic authentication information
               String auth = userName + ":" +  password;   
               String encoded = new sun.misc.BASE64Encoder().encode (auth.getBytes());
               // set basic authorization
               this.urlConnection.setRequestProperty ("Authorization", "Basic " + encoded);
               this.headers = headers;
               // seat headers
               if (headers != null) {
                   Iterator<String> it = headers.keySet().iterator();
                   if (it != null) {
                       while (it.hasNext()) {
                           String key = it.next();
                           String value = headers.get(key);
                           this.urlConnection.setRequestProperty (key, value);
                       }
                   }
               }
           } catch (Exception ex) {
               Config.getLogger().log(Level.SEVERE, "Unable to set basic authorization for " + userName, ex);
           }
       }

       /**
        * private method to get the URLConnection
        * @param str URL string
        */
       private HttpURLConnection getURLConnection(String str) 
           throws MalformedURLException {
           try {
               if (isHttps) {
                   /* when communicating with the server which has unsigned or invalid
                    * certificate (https), SSLException or IOException is thrown.
                    * the following line is a hack to avoid that
                    */
                   Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
                   System.setProperty("java.protocol.handler.pkgs", 
                                      "com.sun.net.ssl.internal.www.protocol");
                   if (isProxy) {
                       System.setProperty("https.proxyHost", proxyHost);
                       System.setProperty("https.proxyPort", proxyPort + "");
                   }
               } else {
                   if (isProxy) {
                       System.setProperty("http.proxyHost", proxyHost);
                       System.setProperty("http.proxyPort", proxyPort + "");
                   }
               }
               URL url = new URL(str);
               HttpURLConnection uc = (HttpURLConnection)url.openConnection();
               // if this header has not been set by a request set the user agent.
               if (headers == null ||
                  (headers != null &&  headers.get("user-agent") == null)) {
                   // set user agent to mimic IE
                   String ua="Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; .NET CLR 1.1.4322)";
                   uc.setRequestProperty("user-agent", ua);  
               }       
               return uc;
           } catch (MalformedURLException me) {
               throw new MalformedURLException(str + " is not a valid URL");
           } catch (Exception e) {
               e.printStackTrace();
               return null;
           }
       }

       /**
        * returns the InputStream from URLConnection
        * @return InputStream
        */
       public InputStream getInputStream() {
           try {
               return (this.urlConnection.getInputStream());
           } catch (Exception e) {
               e.printStackTrace();
               return null;
           }
       }

       /**
        * return the OutputStream from URLConnection
        * @return OutputStream
        */
       public OutputStream getOutputStream() {

           try {
               return (this.urlConnection.getOutputStream());
           } catch (Exception e) {
               e.printStackTrace();
               return null;
           }
       }

       public String getContentEncoding() {
           if (this.urlConnection == null) return null;
           return (this.urlConnection.getContentEncoding());
       }
       public int getContentLength() {
           if (this.urlConnection == null) return -1;
           return (this.urlConnection.getContentLength());
       }

       public String getContentType() {
           if (this.urlConnection == null) return null;
           return (this.urlConnection.getContentType());
       }

       public long getDate() {
           if (this.urlConnection == null) return -1;
           return (this.urlConnection.getDate());
       }

       public String getHeader(String name) {
           if (this.urlConnection == null) return null;
           return (this.urlConnection.getHeaderField(name));
       }

       public long getIfModifiedSince() {
           if (this.urlConnection == null) return -1;
           return (this.urlConnection.getIfModifiedSince());
       }

}
