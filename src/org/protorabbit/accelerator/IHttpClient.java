package org.protorabbit.accelerator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public interface IHttpClient {

    /**
     * return the OutputStream from URLConnection
     * @return OutputStream
     */

    public void setUserName(String userName);

    public void setProxyPort(int port);
 
    public void setProxyServer(String name);

    public void setHeaders(Map<String, String> headers);

    public void setURL(String url);

    public void setPassword(String password);

    public OutputStream getOutputStream() throws IOException;

    public InputStream getInputStream() throws IOException;

    public String getContentEncoding();

    public int getContentLength();

    public String getContentType();

    public String getHeader(String name);

    public String getMethod();

    public void setMethod(String method);

    public long getIfModifiedSince();

}