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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.protorabbit.Config;
import org.protorabbit.json.JSONUtil;
import org.protorabbit.model.ICommand;
import org.protorabbit.model.IContext;
import org.protorabbit.model.IEngine;
import org.protorabbit.model.IParameter;
import org.protorabbit.model.ITemplate;
import org.protorabbit.model.IDocumentContext;

/*
 *  DefaultEngine.java
 *  
 *  This class parses the template file and then calls the corresponding Command Handlers to process
 *  different portions of the page.
 * 
 */
public class DefaultEngine implements IEngine {

    private static Logger logger = null;

    static final Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("org.protrabbit");
        }
        return logger;
    }

    public void renderTemplate(String tid, IContext ctx, OutputStream out) {
        long startTime = (new Date()).getTime();
        Config cfg = ctx.getConfig();
        ctx.setTemplateId(tid);
        ITemplate template = cfg.getTemplate(tid);
        DocumentContext dc = getDocumentContext(template,ctx);
        // go through all the commands and build up the buffers
        // before
        if (dc.getBeforeCommands() != null) {
            processCommands( ctx, dc.getBeforeCommands());
        }
        // default commands
        if (dc.getDefaultCommands() != null) {
            processCommands( ctx, dc.getDefaultCommands());
        }
        // after commands
        if (dc.getAfterCommands() != null) {
            processCommands( ctx, dc.getAfterCommands());
        }
        renderCommands(dc.getAllCommands(), dc.getDocument(),ctx,out);
        resetCommands(dc.getAllCommands());
        long stopTime = (new Date()).getTime();
        getLogger().info("Render time=" + (stopTime - startTime) + "ms");
    }

    private DocumentContext getDocumentContext(ITemplate template, IContext ctx) {
        DocumentContext dc = null;
        if (template.getDocumentContext() == null) {
            dc = new DocumentContext();
            StringBuffer buff = template.getContent(ctx);
            dc.setDocument(buff);
            gatherCommands(buff,ctx,dc);
            template.setDocumentContext(dc);
        } else {
            dc = template.getDocumentContext();
        }
        return dc;
    }

    private void resetCommands(List<ICommand> cmds) {
        if (cmds == null) {
            return;
        }
        for (ICommand c : cmds) {
            if (c.getDocumentContext() != null) {
                List<ICommand> scmds = c.getDocumentContext().getAllCommands();
                resetCommands(scmds);
            }
            c.reset();
        }
        
    }

    @SuppressWarnings("unchecked")
    public List<ICommand> gatherCommands(StringBuffer buff, IContext ctx, DocumentContext dc) {
        List<ICommand> cmds = null; 
        try {
              cmds = getCommands(ctx.getConfig(), buff);
              dc.setAllCommands(cmds);
              List<ICommand> firstCmds = null;
              List<ICommand> lastCmds = (List<ICommand>)ctx.getAttribute(LAST_COMMAND_LIST);
             if (lastCmds == null) {
                 lastCmds = new ArrayList<ICommand>();
                 ctx.setAttribute(LAST_COMMAND_LIST, lastCmds);
             }
             dc.setAfterCommands(lastCmds);
             List<ICommand> defaultCmds = (List<ICommand>)ctx.getAttribute(DEFAULT_COMMAND_LIST);
             if (defaultCmds == null) {
                 defaultCmds = new ArrayList<ICommand>();
                 ctx.setAttribute(DEFAULT_COMMAND_LIST, defaultCmds);
             }
             dc.setDefaultCommands(defaultCmds);
             if (cmds != null) {
                   // pre-process to find first and last commands

                 for (ICommand c : cmds) {

                      if (c.getProcessOrder() == ICommand.PROCESS_FIRST) {
                          if (firstCmds == null) {
                              firstCmds = new ArrayList<ICommand>();
                              dc.setBeforeCommands(firstCmds);
                          }
                          firstCmds.add(c);
                      } else if (c.getProcessOrder() == ICommand.PROCESS_LAST) {

                          lastCmds.add(c);
                      } else {

                          defaultCmds.add(c);
                      }
                  }
              }
          } catch (Exception e) {
              getLogger().log(Level.SEVERE, "Error gathering commands ", e);
          }
          return cmds;
    }

     public void renderCommands(List<ICommand> cmds, StringBuffer buff, IContext ctx, OutputStream out) {
         if (cmds == null) {
             getLogger().warning("List<ICommand passed in as null");
             return;
         }
         try {
             int index = 0;
             for (ICommand c : cmds) {
                    // output everything before the first command
                    out.write(buff.substring(index, c.getStartIndex()).getBytes());
                    index = c.getEndIndex();
                    try {
                        // if we have a sub document context render it
                        IDocumentContext dc = c.getDocumentContext();
                        if (dc != null) {
                            if (dc.getAllCommands() == null ||
                                dc.getAllCommands() != null && dc.getAllCommands().size() == 0) {
                                out.write(dc.getDocument().toString().getBytes());
                            }
                            renderCommands(dc.getAllCommands(), dc.getDocument(), ctx, out) ;
                        }
                        ByteArrayOutputStream bos = c.getBuffer();
                        if (bos != null) {
                            out.write(bos.toByteArray());
                        } else {
                            getLogger().log(Level.SEVERE, "Error rendering buffer of command " + c);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
             }
           // now write everything after the last command
           if (cmds.size() > 0) {
               ICommand lc = cmds.get(cmds.size() -1);
               out.write(buff.substring(lc.getEndIndex()).getBytes());
           }

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error rendering ", e);
        }

        try {
            out.flush();
            out.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    void processCommands(IContext ctx,
                         List<ICommand> cmds) {
        if (cmds == null) {
            return;
        }
        for (ICommand c : cmds) {
            c.setContext(ctx);
            try {
                c.doProcess();
                IDocumentContext dc = c.getDocumentContext();
                if (dc != null) {
                    // make sure sub document commands are processed
                    processCommands(ctx, dc.getBeforeCommands());
                    processCommands(ctx, dc.getDefaultCommands());
                    processCommands(ctx, dc.getAfterCommands());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        
        if (args.length < 1) {
            System.out.println("Usage: --template [template name] templateId");
            System.exit(0);
        }
        
        long startTime = (new Date()).getTime();

        ArrayList<String> cTemplates = new ArrayList<String>();

        String documentRoot = "";

        for (int i=0; i <= args.length - 1; i++) {
            System.out.println("Processing " + args[i]);
            if ("-template".equals(args[i])) {
                cTemplates.add(args[i + 1]);
                i+=1;
            } else if ("-documentRoot".equals(args[i])) {
                documentRoot = args[i+1];
                i+=1;
            }
        }
        Config cfg = new Config();
        FileSystemContext ctx = new FileSystemContext(cfg, documentRoot);
        
        if (cTemplates.size() == 0) {
            System.out.println("Error: You need to specify at least 1 template file.");
            System.exit(0);
        }
        Iterator<String> it = cTemplates.iterator();
        while (it.hasNext()) {
            String ctemplate = it.next();
            int lastSep = ctemplate.lastIndexOf(File.separator);
            String cBase = "";
            if (lastSep != -1) {
                cBase = ctemplate.substring(0, lastSep + 1);
            }
            try {
                JSONObject jo = JSONUtil.loadFromFile(ctemplate);
                cfg.registerTemplates(jo.getJSONArray("templates"), cBase);
                System.out.println("Registered template " + ctemplate + " with baseDir=" + cBase);
            } catch (Exception e) {

            }
        }

        long postConfigTime = (new Date()).getTime();

        // last item is the target;
        String targetTemplate = args[args.length -1];

        getLogger().info("** Config Processing Time : " + (postConfigTime -  startTime) + "\n\n");
        IEngine engine = new DefaultEngine();
        engine.renderTemplate(targetTemplate, ctx, System.out);

        long stopTime = (new Date()).getTime();

        getLogger().info("\n\nRender time for 3 templates=" + (stopTime - postConfigTime) + "ms");
    }

    /*
     * A command looks like 
     * 
     * <^ include("foo") ^> or <^ include("foo", "baz", "bin") ^>
     * <^ insert("bar") ^>
     * 
     */
    public List<ICommand> getCommands(Config cfg, StringBuffer doc) {

        List<ICommand> commands = new ArrayList<ICommand>();
        if (doc == null) return  null;
        int index = 0;
        int len = doc.length();

        while(index < len) {
            index = doc.indexOf("<^", index);
            int end = doc.indexOf("^>", index);

            if (index == -1 || end == -1) {
                break;
            }

            // find the full expression 
            String exp = doc.substring(index + 2, end);

            //find the command
            int paramStart = exp.indexOf("(");
            int paramEnd = exp.lastIndexOf(")");

            if (paramStart != -1 && paramEnd != -1 && paramEnd > paramStart) {

               // get commandType
                String commandTypeString = exp.substring(0,paramStart).trim();

                ICommand cmd = cfg.getCommand(commandTypeString);

                if (cmd != null) {
                    // get the params
                    String paramsString = exp.substring(paramStart +1, paramEnd);

                    // need to parse out JSON
                    IParameter[] params = getParams(paramsString);
                    if (params != null) {
                        cmd.setParams(params);
                    }

                    cmd.setStartIndex(index);
                    cmd.setEndIndex(end + 2);

                    if ("include".equals(commandTypeString) && params.length > 0) {
                        if ("scripts".equals(params[0]) ||
                            "styles".equals(params[0])) {
                            cmd.setType(ICommand.INCLUDE_RESOURCES);
                        } else {
                            cmd.setType(ICommand.INCLUDE);
                        }
                    } else if ("insert".equals(commandTypeString)) {
                        cmd.setType(ICommand.INSERT);
                    } else {
                        cmd.setType(ICommand.CUSTOM);
                    }
                    commands.add(cmd);
                }
            }
            // start the process over
            index = end + 2;
        }
        return commands;
    }

    private static IParameter[] getParams(String paramsString) {

        paramsString = paramsString.trim();
        // for simple case of one item
        if (paramsString.indexOf(",") == -1 &&
            paramsString.startsWith("\"")  &&
            paramsString.endsWith("\"") ) {
            String text = paramsString.substring(0,paramsString.length());
            IParameter param = getParameter(text, new Character('\"'));
            IParameter[] params = new IParameter[1];
            params[0] = param;
            return params;
        }

        // otherwise we have quoted text or JSON objects
        ArrayList<IParameter> params = new ArrayList<IParameter>();
        int index = 0;
        int paramStart = index;
        boolean inQuote = false;
        boolean inArray = false;
        boolean inObject = false;
        int braceDepth = 0;
        int arrayDepth = 0;
        Character lastToken = null;
        while (index  < paramsString.length()) {
            char c = paramsString.charAt(index);
            switch (c) {
                // string
                case '\"' : {
                    if (inQuote) {
                        // if not escaped end the quote
                        if (index > 0 && paramsString.charAt(index-1) != '\\') {
                            inQuote = false;
                            lastToken = new Character('\"');
                        }
                    } else if (!inArray && !inObject){
                        inQuote = true;
                        lastToken = new Character('\"');
                    }
                    break;
                    }
                case '[' : {
                    if (!inQuote) {
                        arrayDepth+=1;
                        lastToken = new Character('[');
                        inArray = true;
                    }
                    break;
                }
                case ']' : {
                    if (!inQuote) {
                        arrayDepth-=1;
                        if (arrayDepth == 0 && braceDepth == 0) {
                            inArray = false;
                            String text = paramsString.substring(paramStart, index + 1).trim();
                            IParameter param = getParameter(text, lastToken);
                            params.add(param);
                            paramStart = index+1;
                            lastToken = new Character(']');
                        }
                    }
                    break;
                }
                case '{' : {
                    if (!inQuote) {
                        if (braceDepth == 0) {
                            inObject = true;
                            paramStart = index;
                            lastToken = new Character('{');
                        }
                        braceDepth+=1;
                    }
                    break;
                }
                case '}' : {
                    if (!inQuote) {
                        braceDepth-=1;
                        lastToken = new Character('}');
                    }
                    if (braceDepth == 0 && arrayDepth == 0 &&
                        lastToken != null && 
                        lastToken.charValue() != ']' && 
                        lastToken.charValue() != '}') {
                        inObject = false;
                        String text = paramsString.substring(paramStart, index +1).trim();
                        IParameter param = getParameter(text, lastToken);
                        params.add(param);
                        paramStart = index+1;
                        lastToken = null;
                    }
                    break;
                }
                // could be a number or boolean
                case ',' : {
                    if (!inQuote && !inArray && !inObject) {
                        String text = paramsString.substring(paramStart, index).trim();
                        IParameter param = getParameter(text, lastToken);
                        params.add(param);
                        paramStart = index+1;
                        lastToken = null;
                    }
                    break;
                }
            } 
            index += 1;
        }
        // get the trailing param if there is one
        if (paramStart < paramsString.length()) {
            String text = paramsString.substring(paramStart, paramsString.length());
            IParameter param = getParameter(text,lastToken);
            params.add(param);
        }
        IParameter[] a = new IParameter[params.size()];
        return params.toArray(a);
    }

    static IParameter getParameter(String text, Character lastToken) {
        Object value = null;
        text = text.trim();
        int type = -1;
        if (lastToken != null &&
            lastToken.charValue() == '\"') {
            type = IParameter.STRING;
            // set the value to not include the quotes
            value = text.substring(1, text.length()-1);
        } else if ("true".equals(text) ||
                   "false".equals(text) ) {
            type = IParameter.BOOLEAN;
            value = new Boolean(("true".equals(text)));
        } else if ("null".equals(text)) {
            type = IParameter.NULL;
        } else if (text.startsWith("{") &&
                text.endsWith("}")) {
            try {
                value = new JSONObject(text);
                type = IParameter.OBJECT;
            } catch (JSONException e) {
                throw new RuntimeException("Error parsing JSON parameter " + text);
            }
        } else if (text.startsWith("[") &&
                text.endsWith("]")) {
            try {
                value = new JSONArray(text);
                type = IParameter.ARRAY;
            } catch (JSONException e) {
                throw new RuntimeException("Error parsing JSON parameter " + text);
            }
        } else {
            try {
                NumberFormat nf = NumberFormat.getInstance();
                value = nf.parse(text);
                type = IParameter.NUMBER;
            } catch(ParseException pe) {
                // do nothing
            }
        }
        if (type != -1) {
            return new Parameter(type, value);
        } else {
            throw new RuntimeException("Error parsing parameter " + text);
        }
    }
}
