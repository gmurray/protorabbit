package org.protorabbit.model.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.protorabbit.model.IContext;
import org.protorabbit.model.ITemplate;

public class URIResourceManager {
	
    @SuppressWarnings("unchecked")
    public static String generateReferences(ITemplate template, IContext ctx, List<ResourceURI> resources, int type) {
        String buff = "";

            if (resources != null) {
                List<String> deferredScripts = (List<String>)ctx.getAttribute(IncludeCommand.DEFERRED_SCRIPTS);
                Iterator<ResourceURI> it = resources.iterator();
                while (it.hasNext()) {

                    ResourceURI ri = it.next();
                    if (ri.isWritten()) {               
                         continue;
                    }
                    String resource = ri.getUri();
                    String baseURI =  ctx.getContextRoot();

                    if (!ri.isExternal()){
                        // map to root
                        if (resource.startsWith("/")) {
                            baseURI = ctx.getContextRoot();
                        } else {
                           baseURI +=  ri.getBaseURI();
                        }
                    } else {
                        baseURI = "";
                    }
                    if (type == ResourceURI.SCRIPT) {
                        if (ri.isDefer()) {
                            if (deferredScripts == null) {
                                deferredScripts = new ArrayList<String>();
                            }
                            String fragement = "<script>protorabbit.addDeferredScript('" +
                                     baseURI + resource + "');</script>";
                            deferredScripts.add(fragement);
                            ri.setWritten(true);
                            ctx.setAttribute(IncludeCommand.DEFERRED_SCRIPTS, deferredScripts);
                        } else {
                            buff += "<script type=\"text/javascript\" src=\"" + baseURI + resource + "\"></script>\n";
                        }
                    } else if (type == ResourceURI.LINK) {
                        String mediaType = ri.getMediaType();
                        if (mediaType == null){
                            mediaType = ctx.getConfig().getMediaType();
                        }
                        if (ri.isDefer()) {
                            if (deferredScripts == null) {
                                deferredScripts = new ArrayList<String>();
                            }
                            String fragement = "<script>protorabbit.addDeferredStyle('" +
                                                baseURI + resource  + "', '" + mediaType + "');</script>";
                            deferredScripts.add(fragement);
                            ri.setWritten(true);
                            ctx.setAttribute(IncludeCommand.DEFERRED_SCRIPTS, deferredScripts);

                        } else {
                            buff += "<link rel=\"stylesheet\" type=\"text/css\"  href=\"" + baseURI + resource + "\" media=\"" + mediaType + "\" />\n";
                        }
                    }
                }
            }
            return buff;
    }

}
