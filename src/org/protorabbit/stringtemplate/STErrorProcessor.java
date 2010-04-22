package org.protorabbit.stringtemplate;

import org.antlr.stringtemplate.StringTemplateErrorListener;

public class STErrorProcessor implements StringTemplateErrorListener {

    public void error( String msg, Throwable e) {
        // don't fail on an empty template
        if ( msg != null &&
             msg.indexOf( "no text in template" ) != -1 ) {
            return;
        } else {
            throw new StringTemplateParseException( msg, e );
        }
    }

    public void warning(String msg) {
        // don't care about warnings right now
    }

}
