package org.protorabbit.communicator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

public class BaseJSONHandler implements Handler {

    public static final String DEFAULT_MODEL = "model";
    public static final String ERROR = "error";
    public static final String SUCCESS = "success";
    public static final String JSON = "json";
    public static final String HTML = "html";
    public static final String BINARY = "binary";

    protected ServletContext ctx = null;
    protected HttpServletRequest request;
    protected HttpServletResponse response;

    private Object model = null;
    private Collection<String> errors = null;
    protected JSONResponse jr = null;
    protected boolean isPoller = false;

    private static Logger logger = null;

    static final Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("org.protrabbit");
        }
        return logger;
    }

    public BaseJSONHandler() {
        jr = new JSONResponse();
    }

    public void addActionError(String error) {
        if (errors == null) {
            errors = new ArrayList<String>();
        }
        errors.add(error);
    }

    public boolean isPoller() {
        return isPoller;
    }
    
    public void setIsPoller(boolean poller) {
        this.isPoller = poller;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
        if ( request != null ) {
            this.ctx = request.getSession().getServletContext();
        }
    }

    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }

    public void setModel(Object model) {
        this.model = model;
    }

    public String doExecute() throws IOException {

        return SUCCESS;

    }

    public String processRequest(JSONObject args) {

        return null;
    }

    public Object getModel() {
        return model;
    }

    public Collection<String> getErrors() {
        return errors;
    }

}

