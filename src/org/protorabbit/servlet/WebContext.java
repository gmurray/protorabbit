package org.protorabbit.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.protorabbit.Config;
import org.protorabbit.json.JSONUtil;
import org.protorabbit.model.IContext;

public class WebContext implements IContext {

    private Config cfg;
    private String templateId;
    private ServletContext sctx;
    private HttpServletRequest req;
    private HttpServletResponse resp;
    private String contextRoot = "";

    public WebContext(Config cfg) {
        this.cfg = cfg;
    }
   
    public WebContext(Config cfg, ServletContext sctx, HttpServletRequest req, HttpServletResponse resp) {
        this.cfg = cfg;
        this.sctx = sctx;
        this.req = req;
        this.resp = resp;
        contextRoot = req.getContextPath();
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }
    
    public void setServletContext(ServletContext sctx) {
        this.sctx = sctx;
    }

    public Config getConfig() {
        return cfg;
    }

    public String getTemplateId() {
        return templateId;
    }

    public boolean resourceExists(String name) {
        URL url = null;
        try {
            url = sctx.getResource(name);
        } catch (MalformedURLException e) {
        }
        return (url != null);
    }

    public boolean isUpdated(String name, long lastUpdate) {
        URL url = null;
        try {
            url = sctx.getResource(name);
            URLConnection uc = url.openConnection();
            long lastMod = uc.getLastModified();
            return (lastMod > lastUpdate);
        } catch (MalformedURLException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public StringBuffer getResource(String baseDir, String name) throws IOException {
        if (name.endsWith(".jsp")) {

                try {

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    BufferedServletResponse br = new BufferedServletResponse(resp, bos);
                    req.getRequestDispatcher(baseDir + name).include(req, br);
                    br.flushBuffer();
                    StringBuffer buff = new StringBuffer(bos.toString(cfg.getEncoding()));
                    bos.close();

                    return buff;

                } catch (Throwable t) {
                    t.printStackTrace();
                }

        } else {

            InputStream is = sctx.getResourceAsStream(baseDir + name);
            String contents = JSONUtil.loadStringFromInputStream(is);
            if (contents != null) {
                return new StringBuffer(contents);
            }
        }
        return null;
    }

    public String getContextRoot() {
        return contextRoot;
    }

    /*
     * Test wether an expression matches the user agent
     * (non-Javadoc)
     * @see org.protorabbit.model.IContext#uaTest(java.lang.String)
     */
    public boolean uaTest(String test) {
        boolean matches = false;
        String userAgent = req.getHeader("User-Agent");
        if (userAgent != null) {
            Pattern p = Pattern.compile(test);
            Matcher m = p.matcher(userAgent);
            return m.find();
        }
        return matches;
    }
}
