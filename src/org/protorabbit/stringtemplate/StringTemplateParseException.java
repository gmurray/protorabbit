package org.protorabbit.stringtemplate;

public class StringTemplateParseException extends RuntimeException {

    private static final long serialVersionUID = 8529935455218219255L;
    private String errorMessage = null;

    public StringTemplateParseException( String error, Throwable parent ) {
        super( error, parent );
        if ( parent != null ) {
            this.errorMessage = parent.toString();
        } else {
            errorMessage = error;
        }
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
