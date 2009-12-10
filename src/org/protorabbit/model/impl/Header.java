package org.protorabbit.model.impl;

import org.protorabbit.accelerator.IHeader;

public class Header implements IHeader {

    String key = null;
    String value = null;

    public Header( String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

}
