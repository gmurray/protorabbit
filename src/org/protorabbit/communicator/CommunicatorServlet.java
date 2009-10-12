package org.protorabbit.communicator;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



@SuppressWarnings("serial")
public class CommunicatorServlet extends HttpServlet {

    protected ServletContext ctx;
    protected PollManager    pm = null;
    private HandlerFactory   hf = null;

    public void init(ServletConfig cfg) {
        try {
            super.init(cfg);
            this.ctx = cfg.getServletContext();
            this.hf = new HandlerFactory(this.ctx);
            pm = new PollManager(this.ctx);
            // test
            hf.addSearchPackage("org.protorabbit.communicator.test");
        } catch (ServletException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // TODO Auto-generated method stub
        super.doDelete(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        // default is to return the messages
        hf.processRequest(request, response);
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // TODO Auto-generated method stub
        super.doHead(req, resp);
    }

    @Override
    protected void doOptions(HttpServletRequest arg0, HttpServletResponse arg1)
            throws ServletException, IOException {

        super.doOptions(arg0, arg1);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        hf.processRequest(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        super.doPut(req, resp);
    }

    @Override
    protected void doTrace(HttpServletRequest arg0, HttpServletResponse arg1)
            throws ServletException, IOException {
        super.doTrace(arg0, arg1);
    }

    @Override
    protected long getLastModified(HttpServletRequest req) {
        // TODO Auto-generated method stub
        return super.getLastModified(req);
    }

}