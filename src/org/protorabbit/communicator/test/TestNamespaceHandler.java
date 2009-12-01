package org.protorabbit.communicator.test;


import org.protorabbit.communicator.BaseJSONHandler;
import org.protorabbit.communicator.Namespace;

@Namespace("/secure")
// test with a URL :
// good: http://localhost:8080/protorabbit/secure/testNamespace!doFoo.action?name=5.2&json={blah:1}
// bad : http://localhost:8080/protorabbit/secure2/testNamespace!doFoo.action?name=5.2&json={blah:1}
public class TestNamespaceHandler extends BaseJSONHandler{
    
    private String name = "not set";

    public void setJson(String name) {
        System.out.println("we got a set with " + name);
        this.name = name;
    }
    
    public void setName(long name) {
        System.out.println("we got a long set with " + name);
    }

    public String doFoo() {
        model = "you got it! " + name;
        addActionError("Foo bar bad");
        return SUCCESS;
    }
}
