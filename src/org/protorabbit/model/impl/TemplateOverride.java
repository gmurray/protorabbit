package org.protorabbit.model.impl;

import org.protorabbit.model.ITestable;

public class TemplateOverride implements ITestable {

    private String test = null;
    private String importURI = null;
    private String uaTest = null;

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }

    public void setImportURI(String importURI) {
        this.importURI = importURI;
    }

    public String getImportURI() {
        return importURI;
    }

    public String getUATest() {
        return uaTest;
    }

    public void setUATest(String test) {
       this.uaTest = test;
    }
    
    public String toString() {
        return "TemplateOverride { test :" + test + ", " +
                  "importURI : " + importURI + ", " +
                  "uaTest : " + uaTest;
    }
}
