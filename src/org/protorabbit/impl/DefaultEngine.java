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

package org.protorabbit.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.protorabbit.Config;
import org.protorabbit.IEngine;
import org.protorabbit.json.JSONUtil;
import org.protorabbit.model.ICommand;
import org.protorabbit.model.IContext;
import org.protorabbit.model.IParameter;
import org.protorabbit.model.ITemplate;
import org.protorabbit.model.impl.FileSystemContext;
import org.protorabbit.model.impl.ParameterImpl;

/*
 *  DefaultEngine.java
 *  
 *  This class parses the template file and then calls the corresponding Command Handlers to process
 *  different portions of the page.
 * 
 */
public class DefaultEngine implements IEngine {

    private static Logger logger = null;

    public static final Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("org.protrabbit");
        }
        return logger;
    }

    public void renderTemplate(String tid, Config cfg, OutputStream out, IContext ctx) {

        long startTime = (new Date()).getTime();

        try {

            ctx.setTemplateId(tid);
            ITemplate template = cfg.getTemplate(tid);

            StringBuffer buff = template.getContent(ctx);
            List<ICommand> cmds = getCommands(cfg, buff, template.getJSON());
            List<ICommand> firstCmds = null;
            List<ICommand> lastCmds = null;
            List<ICommand> defaultCmds = null;
            HashMap<Integer, ByteArrayOutputStream> buffers = null;
            if (cmds != null) {
                  // pre-process to find first and last commands
                int cindex = 0;
                for (ICommand c : cmds) {
                    c.setCommandIndex(cindex++);
                    if (c.getProcessOrder() == ICommand.PROCESS_FIRST) {
                        if (firstCmds == null) {
                            firstCmds = new ArrayList<ICommand>();
                        }
                        firstCmds.add(c);
                    } else if (c.getProcessOrder() == ICommand.PROCESS_LAST) {
                        if (lastCmds == null) {
                            lastCmds = new ArrayList<ICommand>();
                        }
                        lastCmds.add(c);
                    } else {
                        if (defaultCmds == null) {
                            defaultCmds = new ArrayList<ICommand>();
                        }
                        defaultCmds.add(c);
                    }
                }
                if (buffers == null) {
                    buffers = new HashMap<Integer, ByteArrayOutputStream>(); 
                }
                // first
                if (firstCmds != null) {
                    processCommands( ctx, firstCmds, buffers);
                }
                // default commands
                if (defaultCmds != null) {
                    processCommands( ctx, defaultCmds, buffers);
                }
                // last commands
                if (lastCmds != null) {
                    processCommands( ctx, lastCmds, buffers);
                }

                int index = 0;
                for (ICommand c : cmds) {
                    // output everything before the first command
                     out.write(buff.substring(index, c.getStartIndex()).getBytes());
                    index = c.getEndIndex();
                    try {
                        ByteArrayOutputStream bos = buffers.get(new Integer(c.getCommandIndex()));
                        if (bos != null) {
                            out.write(bos.toByteArray());
                        } else {
                            getLogger().log(Level.SEVERE, "Error rendering buffer of commandIndex " + c.getCommandIndex());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
           // now write everything after the last command
           ICommand lc = cmds.get(cmds.size() -1);
           out.write(buff.substring(lc.getEndIndex()).getBytes());

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error rendering ", e);
        }
        long stopTime = (new Date()).getTime();
        getLogger().info(" Render time=" + (stopTime - startTime) + "ms");
        try {
            out.flush();
            out.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    void processCommands(IContext ctx,
                         List<ICommand> cmds,
                         HashMap<Integer, ByteArrayOutputStream> buffers) {

        for (ICommand c : cmds) {
            c.setContext(ctx);
            ByteArrayOutputStream 
                bos = new ByteArrayOutputStream();
            try {
                c.doProcess(bos);
                buffers.put(new Integer(c.getCommandIndex()), bos);
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
        engine.renderTemplate(targetTemplate, cfg, System.out, ctx);

        long stopTime = (new Date()).getTime();

        getLogger().info("\n\nRender time for 3 templates=" + (stopTime - postConfigTime) + "ms");
    }

    /*
     * A command looks like 
     * 
     * <^ include('foo') ^> or <^ include('foo', 'baz', 'bin') ^>
     * <^ insert('bar') ^>
     * 
     */
    public static List<ICommand> getCommands(Config cfg, StringBuffer doc, JSONObject template) {

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
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else if (text.startsWith("[") &&
                text.endsWith("]")) {
            try {
                value = new JSONArray(text);
                type = IParameter.ARRAY;
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
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
            return new ParameterImpl(type, value);
        } else {
            throw new RuntimeException("Error parsing parameter " + text);
        }
    }
}
