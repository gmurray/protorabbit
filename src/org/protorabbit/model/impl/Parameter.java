package org.protorabbit.model.impl;

import org.protorabbit.model.IParameter;

public class Parameter implements IParameter {

    private Object value;
    private int type = -1;

    public Parameter( int type, Object value) {
        this.type = type;
        this.value = value;
    }

    public int getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }
    
    public String toString() {
        return " IParameter : { type : " + type + ", value : " + value + " }";
    }

}
