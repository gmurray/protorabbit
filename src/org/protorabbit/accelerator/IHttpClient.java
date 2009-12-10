package org.protorabbit.accelerator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
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

    public List<IHeader>  getResponseHeaders() throws IOException;

    public String getMethod();

    public void doPost( Map<String, String> params, String contentType ) throws IOException;

    public void doPost( String paramString, String contentType ) throws IOException;

    public void setMethod(String method);

    public long getIfModifiedSince();

}