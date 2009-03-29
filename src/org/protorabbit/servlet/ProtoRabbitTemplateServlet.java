package org.protorabbit.servlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.protorabbit.Config;
import org.protorabbit.Engine;
import org.protorabbit.IOUtil;
import org.protorabbit.accelerator.CacheContext;
import org.protorabbit.accelerator.CacheableResource;
import org.protorabbit.json.JSONUtil;
import org.protorabbit.model.ITemplate;

@SuppressWarnings("serial")
public class ProtoRabbitTemplateServlet extends HttpServlet {

	private ServletContext ctx;
	private Config jcfg;
	private boolean isDevMode = false;
	private HashMap<String, Long> lastUpdated;

	private String[] templates = null;

    // defaults 	
	private String defaultTemplateURI = "/WEB-INF/templates.json";	
	private String serviceURI = "prt";
	private long maxAge = 1225000;

	public void init(ServletConfig cfg) {
		try {
			super.init(cfg);
			lastUpdated = new HashMap<String, Long>();
			this.ctx = cfg.getServletContext();
			if (ctx.getInitParameter("prt-dev-mode") != null) {
				isDevMode = ("true".equals(ctx.getInitParameter(
						"prt-dev-mode").toLowerCase()));
			}
			if (ctx.getInitParameter("prt-service-uri") != null) {
				serviceURI = ctx.getInitParameter("prt-dev-mode");
			}
			
			if (ctx.getInitParameter("prt-max-timeout") != null) {
				String maxTimeoutString =  ctx.getInitParameter("prt-max-timeout");
				try {
				    maxAge = (new Long(maxTimeoutString)).longValue();
				} catch (Exception e) {
    			    System.err.println("Non-fatal: Error processing configuration : prt-service-uri must be a long.");	
				}
			}
			if (ctx.getInitParameter("prt-templates") != null) {
				String tString = ctx.getInitParameter("prt-templates");
				// clean up the templates string
				tString = tString.trim();
				tString = tString.replace(" ", "");
				templates = tString.split(",");
			} else {
				templates = new String[] { defaultTemplateURI };
			}
			updateConfig();
		} catch (ServletException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void updateConfig() throws IOException {
		boolean needsUpdate = false;
		String templateName = null;
		for (int i = 0; i < templates.length; i++) {
			try {
				templateName = templates[i];
				URL turl = ctx.getResource(templateName);
				URLConnection uc = turl.openConnection();
				long lastMod = uc.getLastModified();
				Long lu = lastUpdated.get(templates[i]);
				long lastTime = 0;
				if (lu != null) {
					lastTime = lu.longValue();
				}
				if (lastMod > lastTime) {
					needsUpdate = true;
					break;
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (NullPointerException npe) {
				System.out
						.println("Error reading configuration. Could not find "
								+ templateName);
			}
		}

		if ((jcfg == null || needsUpdate) && templates.length > 0) {
			jcfg = new Config(serviceURI, maxAge);
			for (int i = 0; i < templates.length; i++) {
				JSONObject base = JSONUtil.loadFromInputStream(this.ctx
						.getResourceAsStream(templates[i]));
				String baseURI = getTemplateDefDir(templates[i]);
				try {
					JSONArray templatesArray = base.getJSONArray("templates");
					if (templatesArray != null) {
						jcfg.registerTemplates(templatesArray, baseURI);
					}
					updateLastModified(templates[i]);
					System.out.println("Registered " + templates[i]);
				} catch (JSONException e1) {
					e1.printStackTrace();
				}
			}

		}
	}

	public String getTemplateDefDir(String location) {

		int last = location.lastIndexOf("/");
		if (last != -1) {
			return location.substring(0, last + 1);
		}
		return null;
	}

	void updateLastModified(String uri) throws IOException {
		URL turl = ctx.getResource(uri);
		if (turl != null) {
			URLConnection uc = turl.openConnection();
			long lastMod = uc.getLastModified();
			lastUpdated.put(uri, lastMod);
		} else {
			System.out.println("Error checking for last modified on:  " + uri);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// check for updates to the templates.json file
		if (isDevMode) {
			updateConfig();
		}
		
		boolean canGzip = false;
		// check if client supports gzip
		Enumeration<String> hnum = req.getHeaders("Accept-Encoding");
		while (hnum.hasMoreElements()) {
			String acceptType = hnum.nextElement();
			if (acceptType != null && acceptType.indexOf("gzip") != -1) {
				canGzip = true;
				break;
			}
		}

		String id = req.getParameter("id");
		
		if (id != null) {
			OutputStream out = resp.getOutputStream();
            if (id.length() > 4) {
            	id = id.substring(0, id.length() - 4);
            }
			CacheableResource cr = jcfg.getCombinedResourceManager().getResource(id);
			if (cr == null) {
				System.out.println("could not find resource " + id);
				
			}
			if (cr.getContentType() != null) {
				resp.setContentType(cr.getContentType());
			}
			
			if (cr != null) {
                CacheContext cc = cr.getCacheContext();
				String etag = cr.getContentHash();
				// get the If-None-Match header
				String ifNoneMatch = req.getHeader("If-None-Match");
				if (etag != null &&
					ifNoneMatch != null && 
					ifNoneMatch.equals(etag)) {
					resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				}
				if (etag != null) {
					resp.setHeader("ETag", etag);
				}      
				resp.setHeader("Expires", cc.getExpires());
				resp.setHeader("Cache-Control", "public,max-age=" + cc.getMaxAge());

				if (jcfg.getGzip() && canGzip) {
					resp.setHeader("Content-Encoding", "gzip");

					byte[] bytes = cr.getGZippedContent();
					if (bytes != null) {
						ByteArrayInputStream bis = new ByteArrayInputStream(
								bytes);
						IOUtil.writeBinaryResource(bis, out);
					}
				} else {
					resp.setHeader("Content-Type", cr.getContentType());
					out.write(cr.getContent().toString().getBytes());
				}
				return;
				
			} else {
				System.out.println("resource " + id
						+ " requested but not found.");
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
		}

		String servletPath = req.getServletPath();
		int lastSep = servletPath.lastIndexOf("/");

		if (lastSep != -1 && lastSep < servletPath.length() - 1) {
			int nextDot = servletPath.indexOf(".", lastSep + 1);
			id = servletPath.substring(lastSep + 1, nextDot);
		}
		
		ITemplate t = null;
		
		if (id != null) {
			t = jcfg.getTemplate(id);
		}
		
	    if (id == null || t == null) {
	    	System.out.println("template " + id + " requested but not found.");
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
	    }
		
		// buffer the output steam
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		WebContext wc = new WebContext(jcfg, ctx, req, resp);

		CacheableResource tr = t.getTemplateResource();
		
        if (t.getTimeout() > 0 && (tr == null ||
        		tr.getCacheContext().isExpired() )) {
            
        	if (canGzip) {
    		    resp.setHeader("Vary", "Accept-Encoding");
    		    resp.setHeader("Content-Encoding", "gzip");
        	}
        	
    		resp.setHeader("Expires", IOUtil.getExpires(jcfg.getMaxAge()));
    		resp.setHeader("Cache-Control", "public,max-age=" + IOUtil.getMaxAge(jcfg.getMaxAge()));    
    		
    		resp.setHeader("Content-Type", "text/html");
    		
            // headers after this point do not get written
    		Engine.renderTemplate(id, jcfg, bos, wc);
    		
        	String content = bos.toString("UTF8");
        	String hash = IOUtil.generateHash(content);
            CacheableResource cr = new CacheableResource("text/html", t.getTimeout(), hash);
        	resp.setHeader("ETag", cr.getContentHash());
          
            
            cr.setContent(new StringBuffer(content));
            t.setTemplateResource(cr);
    		
            if (canGzip) {
	    		byte[] bytes = cr.getGZippedContent();
				resp.setContentLength(bytes.length);
				OutputStream out = resp.getOutputStream();
				if (bytes != null) {
					ByteArrayInputStream bis = new ByteArrayInputStream(
							bytes);
					IOUtil.writeBinaryResource(bis, out);
				}
			} else {
				OutputStream out = resp.getOutputStream();
        	    byte[] bytes = cr.getContent().toString().getBytes();
        	    resp.setContentLength(bytes.length);
				if (bytes != null) {
					ByteArrayInputStream bis = new ByteArrayInputStream(
							bytes);
					IOUtil.writeBinaryResource(bis, out);
				} 
            }

            // write out content / gzip or otherwise from the cache
        } else if (t.getTimeout() > 0 && tr != null) {
  	
        	// if the client has the same resource as the one on the server return a 304
    		// get the If-None-Match header
        	String etag = tr.getContentHash();
        	
    		String ifNoneMatch = req.getHeader("If-None-Match");
    		if (etag != null && ifNoneMatch != null &&
    			ifNoneMatch.equals(etag)) {
    			resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
    			return;
    		} 
    		resp.setContentType(tr.getContentType());
    		resp.setContentLength(tr.getGZippedContent().length);
        	resp.setHeader("ETag", etag);
    		resp.setHeader("Expires", tr.getCacheContext().getExpires());
    		resp.setHeader("Cache-Control", "public,max-age=" + tr.getCacheContext().getMaxAge());
    		canGzip = true;
    		
        	if (canGzip) {
        		
        		resp.setHeader("Content-Encoding", "gzip");
				byte[] bytes = tr.getGZippedContent();
				resp.setContentLength(bytes.length);
				OutputStream out = resp.getOutputStream();
				if (bytes != null) {
					ByteArrayInputStream bis = new ByteArrayInputStream(
							bytes);
					IOUtil.writeBinaryResource(bis, out);
				}   
        	} else {
        		OutputStream out = resp.getOutputStream();
        	    byte[] bytes =tr.getContent().toString().getBytes();
        	    resp.setContentLength(bytes.length);
				if (bytes != null) {
					ByteArrayInputStream bis = new ByteArrayInputStream(
							bytes);
					IOUtil.writeBinaryResource(bis, out);
				}   	
        	}

        } else {
    		OutputStream out = resp.getOutputStream();
    		Engine.renderTemplate(id, jcfg, bos, wc);	
		    out.write(bos.toByteArray());
        }

	}
}