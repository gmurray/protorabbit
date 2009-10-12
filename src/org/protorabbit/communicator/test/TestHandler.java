package org.protorabbit.communicator.test;

import org.protorabbit.communicator.BaseJSONHandler;

public class TestHandler extends BaseJSONHandler{
    
    private String name = "not set";

    public void setName(String name) {
        System.out.println("we got a set with " + name);
        this.name = name;
    }

    public String doFoo() {
        model = "you got it! " + name;
        addActionError("Foo bar bad");
        return SUCCESS;
    }
}
