package org.protorabbit.communicator.test;

import org.protorabbit.communicator.BaseJSONHandler;

// test with a URL :
//  http://localhost:8080/protorabbit/secure/test!doFoo.action?name=5.2&json={blah:1}
public class TestAction extends BaseJSONHandler{
    
    private String name = "not set";

    public void setJson(String name) {
        System.out.println("we got a setJson with " + name);
        this.name = name;
    }
    
    public void setName(String name) {
        System.out.println("we got a setName string set with " + name);	
    }

    public void setName(long name) {
        System.out.println("we got a setName long set with " + name);
    }
    
    public String doFoo() {
        setModel( "you got it! " + name );
        addActionError("Foo bar bad");
        return SUCCESS;
    }
}