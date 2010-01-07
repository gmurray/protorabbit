package org.protorabbit.model.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.protorabbit.Config;
import org.protorabbit.json.JSONUtil;
import org.protorabbit.model.IEngine;

public class TextEngine {

    private static Logger logger = null;

    static final Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("org.protrabbit");
        }
        return logger;
    }

    /**
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {

        if (args.length < 1) {
            System.out.println("Usage: --templateDef [template name] templateId");
            System.exit(0);
        }

        long startTime = (new Date()).getTime();

        ArrayList<String> cTemplates = new ArrayList<String>();

        String documentRoot = "";

        for (int i=0; i <= args.length - 1; i++) {
            System.out.println("Processing " + args[i]);
            if ("-templateDef".equals(args[i])) {
                cTemplates.add(args[i + 1]);
                i+=1;
            } else if ("-documentRoot".equals(args[i])) {
                documentRoot = args[i+1];
                i+=1;
            }
        }
        Config cfg = Config.getInstance();

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
                getLogger().log( Level.SEVERE, "Error regisering template.", e);
            }
        }

        long postConfigTime = (new Date()).getTime();

        // last item is the target;
        String targetTemplate = args[args.length -1];

        getLogger().info("** Config Processing Time : " + (postConfigTime -  startTime) + "\n\n");

        IEngine engine = new DefaultEngine();
        for (int i=0; i < 1000; i++) { 
            FileSystemContext ctx = new FileSystemContext(cfg, documentRoot);
            ctx.setAttribute("title", "foo " + i);
            engine.renderTemplate(targetTemplate, ctx, System.out);
            System.out.println("\n\n*******");
            FileSystemContext ctx2 = new FileSystemContext(cfg, documentRoot);
            ctx2.setAttribute("title", "bar " + i);
            engine.renderTemplate(targetTemplate, ctx2, System.out);
        }
        long stopTime = (new Date()).getTime();

       System.out.println("\n\nRender time : " + (stopTime - postConfigTime) + "ms");
    }
}
