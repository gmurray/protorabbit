package org.protorabbit.accelerator.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.*;
import java.util.Map.Entry;
import java.util.logging.*;
import java.security.Security;

import org.protorabbit.accelerator.IHeader;
import org.protorabbit.accelerator.IHttpClient;
import org.protorabbit.model.impl.Header;

   /**
    *
    * HTTPClient supporting both http and https and 
    *  with the ability to set pass in headers and set
    *  as a proxy port / host
    */
    public class HttpClient implements IHttpClient {
       
        private String url;
        private String method = "GET";
        private String proxyHost = null;
        private int proxyPort = -1;
        private String userName = null;
        private String password = null;
        private boolean isHttps = false;
        private HttpURLConnection urlConnection = null;
        private Map<String,String> headers;
       // fake as IE 8
       private String defaultUserAgent = "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.0)";
       private static Logger logger = null;

       public static Logger getLogger() {
           if (logger == null) {
               logger = Logger.getLogger("org.protrabbit");
           }
           return logger;
       }

       public HttpClient() {
       }


       /**
        * @param phost PROXY host name
        * @param pport PROXY port string
        * @param url URL string
        * @param headers Map
        * @throws IOException 
        */
       void init()
           throws IOException {

           if (url.trim().startsWith("https:")) {
               isHttps = true;
           }
           if (proxyHost != null) {
               System.setProperty("https.proxyHost", proxyHost);
           }
           if (proxyPort != -1 ) {
               System.setProperty("https.proxyPort", proxyPort + "");
           }
           this.urlConnection = getURLConnection(url);
           try {
               this.urlConnection.setRequestMethod(method);
           } catch (java.net.ProtocolException pe) {
               getLogger().log(Level.SEVERE, "Unable protocol method to " + method, pe);
           }
           if (userName != null && password != null) {
               // Send HTTP Basic authentication information
               String auth = userName + ":" +  password;
               String encoded = new sun.misc.BASE64Encoder().encode (auth.getBytes());
               // set basic authorization
               this.urlConnection.setRequestProperty ("Authorization", "Basic " + encoded);
           }

       }

       /**
        * private method to get the URLConnection
        * @param str URL string
        * @throws IOException 
        */
       private HttpURLConnection getURLConnection(String str) 
           throws IOException {
           try {
               if (isHttps) {
                    // Prevent unassigned ssl certificate SSLException / IOException exceptions 
                   Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
                   System.setProperty("java.protocol.handler.pkgs", 
                                      "com.sun.net.ssl.internal.www.protocol");
               }
               URL url = new URL(str);
               HttpURLConnection uc = (HttpURLConnection)url.openConnection();
               // if this header has not been set by a request set the user agent.
               if (headers == null ||
                  (headers != null &&  headers.get("user-agent") == null)) {
                   // set user agent
                   uc.setRequestProperty("user-agent",defaultUserAgent);
               }       
               return uc;
           } catch (MalformedURLException me) {
               throw new MalformedURLException(str + " is not a valid URL");
           }
       }

       public InputStream getInputStream() throws IOException {
           if (url == null) {
               throw new RuntimeException("url must be set before getting input stream");
           }
           init();
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
           try {
               return (this.urlConnection.getInputStream());
           } catch (Exception e) {
               e.printStackTrace();
               return null;
           }
       }

       @SuppressWarnings("unchecked")
    public void doPost(Map<String, String> postMap, String contentType) throws IOException {
           String body = "";
           Iterator ks = postMap.entrySet().iterator();
           //for ( Entry k : ks ) {
               while ( ks.hasNext() ) {
                   Entry k = (Entry)ks.next();
               body += k.getKey() + "=" + k.getValue();
               if (ks.hasNext()) {
                   body += "&";
               }
           }
           doPost( body, contentType );
       }

       public void doPost(String postData, String contentType) throws IOException {
           if (this.urlConnection == null ) {
               init();
           }
           this.urlConnection.setDoOutput(true);
           if (contentType != null) {
               this.urlConnection.setRequestProperty( "Content-type", contentType );
           }
                  
           OutputStream os = this.getOutputStream();
           PrintStream ps = new PrintStream(os);
           ps.print(postData);
           ps.close(); 
       }

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

    public String getHeader(String name) {
           if (this.urlConnection == null) return null;
           return (this.urlConnection.getHeaderField(name));
       }

    public long getIfModifiedSince() {
           if (this.urlConnection == null) return -1;
           return (this.urlConnection.getIfModifiedSince());
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setProxyPort(int port) {
      this.proxyPort = port;
    }

    public void setProxyServer(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public void setURL(String url) {
        this.url = url;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<IHeader> getResponseHeaders() throws IOException {
        if (urlConnection == null) {
            init();
            if (urlConnection == null) {
                return null;
            }
        }
        List<IHeader> lhm = new ArrayList<IHeader>();
        for (int i = 0;; i++) {
            String key = urlConnection.getHeaderFieldKey(i);
            String value = urlConnection.getHeaderField(i);
            if (key == null && value == null) {
              break;
            }
            IHeader h = new Header( key, value );
            lhm.add( h );
        }
        return lhm;
    }

}
