/*
 * Protorabbit
 *
 * Copyright (c) 2009 Greg Murray (protorabbit.org)
 * 
 * Licensed under the MIT License:
 * 
 *  http://www.opensource.org/licenses/mit-license.php
 *
 */

package org.protorabbit.model.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.protorabbit.Config;
import org.protorabbit.accelerator.ResourceManager;
import org.protorabbit.accelerator.impl.CacheableResource;
import org.protorabbit.accelerator.impl.DeferredResource;
import org.protorabbit.model.ICommand;
import org.protorabbit.model.IContext;
import org.protorabbit.model.IEngine;
import org.protorabbit.model.IParameter;
import org.protorabbit.model.IProperty;
import org.protorabbit.model.ITemplate;
import org.protorabbit.model.IDocumentContext;
import org.protorabbit.util.IOUtil;

public class IncludeCommand extends BaseCommand {

    public static final String COUNTER = "COUNTER";
    public static final String DEFERRED_SCRIPTS = "DEFERRED_SCRIPTS";

    private static Logger logger = null;

    static final Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("org.protrabbit");
        }
        return logger;
    }

    public IncludeCommand(){
        super();
        // set this command to process after everything else
        setProcessOrder(ICommand.PROCESS_FIRST);
    }

    @SuppressWarnings("unchecked")
    public void doProcess(IContext ctx) throws IOException {

        Config cfg = ctx.getConfig();
        int counter = 0;
        if (ctx.getAttribute(COUNTER) != null) {
            counter = ((Integer)ctx.getAttribute(COUNTER)).intValue();
        } 
        boolean useThreadedDefer = true;

        String tid = ctx.getTemplateId();

        String id = null;
        boolean parseIncludeFile = false;
        if (params.length > 0 && params[0].getType() == IParameter.STRING) {
            id = params[0].getValue().toString();
        } else {
            getLogger().severe("Error processing property " + params[0].getValue().toString() + " Parameter is not of type String");
            return;
        }
        if (params.length > 1 && params[1].getType() == IParameter.BOOLEAN) {
            parseIncludeFile = ((Boolean) params[1].getValue()).booleanValue();
        }

        String resourceName = null;
        String baseDir = null;
        IProperty property = null;

        ITemplate template = cfg.getTemplate(tid);
        if (template != null) {

            property = template.getProperty( id, ctx );

            if (property == null) {
                getLogger().log(Level.FINEST, "Unable to find property " + id + " in template " + tid);
                return;
            }
            if (property.getUATest() != null) {
                if (ctx.uaTest(property.getUATest()) == false) {
                    // track the test
                    ctx.addUAScriptTest(property.getUATest());
                    return;
                }
            }
            if (property.getTest() != null) {
                if (ctx.test(property.getTest()) == false) {
                    return;
                }
            }
            resourceName = property.getValue();

            baseDir = "";
            if (!resourceName.startsWith("/") && !resourceName.startsWith("http")) {
                baseDir = property.getBaseURI();
            }

        }

        StringBuffer buff = new StringBuffer("");
        StringBuffer deferContent = new StringBuffer("");
        if (property.getDefer() != null && property.getDefer().booleanValue() == true){
            List<String> deferredScripts = (List<String>)ctx.getAttribute(DEFERRED_SCRIPTS);
            if (deferredScripts == null) {
                deferredScripts = new ArrayList<String>();
                ctx.setAttribute(DEFERRED_SCRIPTS, deferredScripts);
            }

            String resourceId = "";

            if (useThreadedDefer) {
                if (property.getId() != null) {
                    resourceId = property.getId();
                } else {
                    resourceId = property.getKey();
                }
                ResourceManager crm = cfg.getCombinedResourceManager();
                Long timeout = property.getTimeout();
                if (timeout == null) {
                    timeout = 0L;
                }
                DeferredResource dr = new DeferredResource(baseDir, resourceName, ctx, timeout );
                crm.putResource( ctx.getTemplateId() + "_" + resourceId, dr );
            } else {
                IncludeFile inc = cfg.getIncludeFileContent( ctx.getTemplateId(), id,ctx );
                buff = inc.getContent();
                String hash  = IOUtil.generateHash(buff.toString());
                if (property.getId() != null) {
                    resourceId = property.getId();
                } else {
                    resourceId = property.getKey();
                }
                if (inc.getDeferContent() != null) {
                    deferContent = inc.getDeferContent();
                }
                ResourceManager crm = cfg.getCombinedResourceManager();
                CacheableResource cr = new CacheableResource( "text/html", inc.getTimeout(), hash );
                cr.setContent( buff );
                crm.putResource( ctx.getTemplateId() + "_" + resourceId , cr );
            }

            getBuffer(ctx).write( ("<div id='" + resourceId + "'>" + deferContent.toString() + "</div>").getBytes() );

            String script = "<script>protorabbit.addDeferredFragement({ include : '" + cfg.getResourceService() + 
                       "?resourceid=" + resourceId + ".htm&tid=" + ctx.getTemplateId() + "', elementId : '" + resourceId + "' });</script>";
            deferredScripts.add(script);
            ctx.setAttribute(COUNTER, new Integer(counter + 1));
        } else {
            IncludeFile inc = cfg.getIncludeFileContent(ctx.getTemplateId(), id,ctx);
            if (inc != null) {
                buff = inc.getContent();
            } else {
                 getLogger().severe("Unable to fine Include file with id " + id + ".");
                 return;
            }
            if (parseIncludeFile) {
                IDocumentContext document = new DocumentContext();
                setDocumentContext(document);
                IEngine engine = cfg.getEngine();
                StringBuffer doc = inc.getContent();
                List<ICommand> cmds = engine.getCommands(cfg, doc);
                document.setAllCommands(cmds);
                document.setDocument(doc);
                List<ICommand> lastCmds = null;
                List<ICommand> defaultCmds = null;

                for (ICommand c : cmds) {
                    if ( c.getProcessOrder() == ICommand.PROCESS_DEFAULT ||
                         c.getProcessOrder() == ICommand.PROCESS_FIRST ) {
                        if (defaultCmds == null) {
                            defaultCmds = new ArrayList<ICommand>();
                            document.setAfterCommands(defaultCmds);
                        }
                        defaultCmds.add(c);
                    } else if ( c.getProcessOrder() == ICommand.PROCESS_LAST ) {
                        if ( lastCmds == null ) {
                            lastCmds = new ArrayList<ICommand>();
                            document.setAfterCommands( lastCmds );
                        }
                        lastCmds.add( c );
                    } else {
                        getLogger().warning("Unable to process command " + c +
                                            ". Command processOrder must be of type ICommand.PROCESS_DEFAULT or ICommand.PROCESS_LAST. Command type is " + c.getProcessOrder());
                    }
                }
            } else {
                getBuffer(ctx).write(buff.toString().getBytes());
            }
        }

    }
}