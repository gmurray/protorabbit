package org.protorabbit.model.impl;

import org.protorabbit.model.IParameter;

public class ParameterImpl implements IParameter {

    private Object value;
    private int type = -1;

    public ParameterImpl( int type, Object value) {
        this.type = type;
        this.value = value;
    }

    public int getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

}
