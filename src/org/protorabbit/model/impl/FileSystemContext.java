package org.protorabbit.model.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.protorabbit.Config;
import org.protorabbit.IOUtil;

public class FileSystemContext extends BaseContext {

    Config cfg;
    private String contextRoot = "";
    
    public FileSystemContext(Config cfg, String contextRoot) {
        this.cfg = cfg;
        this.contextRoot = contextRoot;
    }

    public Config getConfig() {
        return cfg;
    }

    public StringBuffer getResource(String baseDir, String name) throws IOException {
        // / signifies the base directory
        if (name.startsWith("/")) {
            name = name.substring(1);
            baseDir = contextRoot;
        }
        StringBuffer contents = IOUtil.loadStringFromInputStream(new FileInputStream(baseDir + name), cfg.getEncoding());
         return contents;
    }

    public boolean resourceExists(String name) {
        File f = new File(name);
        return f.exists();
    }

    public String getContextRoot() {
        return contextRoot;
    }

    public boolean isUpdated(String name, long lastUpdate) {
        File f = new File(name);
        long lastModified = f.lastModified();
        return (lastUpdate > lastModified);
    }

    public boolean uaTest(String test) {
        return true;
    }

}
