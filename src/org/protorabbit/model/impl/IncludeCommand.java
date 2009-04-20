package org.protorabbit.model.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.protorabbit.Config;
import org.protorabbit.IOUtil;
import org.protorabbit.accelerator.CacheableResource;
import org.protorabbit.accelerator.CombinedResourceManager;

public class IncludeCommand extends BaseCommand {

    public static final String COUNTER = "COUNTER";
    public static final String DEFERRED_SCRIPTS = "DEFERRED_SCRIPTS";

    @Override
    public void doProcess(OutputStream out) throws IOException {

        Config cfg = ctx.getConfig();
        int counter = 0;
        if (ctx.getAttribute(COUNTER) != null) {
            counter = ((Integer)ctx.getAttribute(COUNTER)).intValue();
        } 
        
        IncludeFile inc = cfg.getIncludeFileContent(ctx.getTemplateId(), params[0],ctx);

        StringBuffer buff = new StringBuffer("");
        buff = inc.getContent();
        if (inc.isDefer()){
            List<String> deferredScripts = (List)ctx.getAttribute(DEFERRED_SCRIPTS);
            if (deferredScripts == null) {
                deferredScripts = new ArrayList<String>();
                ctx.setAttribute(DEFERRED_SCRIPTS, deferredScripts);
            }
            String hash = IOUtil.generateHash(buff.toString());
            String resourceId = hash + "_" + counter;
            CombinedResourceManager crm = cfg.getCombinedResourceManager();
            CacheableResource cr = new CacheableResource("text/html", inc.getTimeout(), hash);
            cr.setContent( buff );
            crm.putResource(resourceId, cr);
            StringBuffer deferContent = new StringBuffer("");
            if (inc.getDeferContent() != null) {
                deferContent = inc.getDeferContent();
            }
            out.write(("<div id='" + resourceId + "'>" + deferContent.toString() + "</div>").getBytes());

            String script = "<script>protorabbit.addDeferredFragement({ include : '" + cfg.getResourceService() + 
                       "?id=" + resourceId + ".htm', elementId : '" + resourceId + "' });</script>";
            deferredScripts.add(script);
            ctx.setAttribute(COUNTER, new Integer(counter + 1));
        } else {
            out.write(buff.toString().getBytes());
        }
        out.flush();
    }
}