package org.protorabbit.communicator;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

public interface Handler {

    public String processRequest(JSONObject args);
    public void setRequest(HttpServletRequest request);
    public void setResponse(HttpServletResponse response);
    public String doExecute() throws IOException;
    public boolean isPoller();
    public Object getModel();
    public Collection<String> getErrors();

}
