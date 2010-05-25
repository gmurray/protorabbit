package org.protorabbit.stringtemplate;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.antlr.stringtemplate.AttributeRenderer;

public class DateAttributeRenderer implements AttributeRenderer {

    public String toString( Object o, String format ) {
        Date d = (Date) o;
        SimpleDateFormat f = new SimpleDateFormat(format);
        return f.format(d);
    }

    public String toString( Object o ) {
        Date d = (Date) o;
        SimpleDateFormat f = new SimpleDateFormat("MM/dd/yyyy");
        return f.format(d);
    }

}
