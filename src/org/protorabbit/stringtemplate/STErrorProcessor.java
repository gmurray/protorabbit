package org.protorabbit.stringtemplate;

import org.antlr.stringtemplate.StringTemplateErrorListener;

public class STErrorProcessor implements StringTemplateErrorListener {

    public void error( String msg, Throwable e) {
        throw new StringTemplateParseException( msg, e );
    }

    public void warning(String msg) {
        // don't care about warnings right now
    }

}
