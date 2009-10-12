package org.protorabbit.communicator;

public class MappingObject {

    public final static int APPLICATION = 1;
    public final static int REQUEST     = 2;
    public final static int SESSION     = 3;

    private String          name        = null;
    private int             scope       = -1;

    public MappingObject(String name, int scope) {
        this.name = name;
        this.scope = scope;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getScope() {
        return scope;
    }

    public void setScope(int scope) {
        this.scope = scope;
    }

}
