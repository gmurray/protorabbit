package org.protorabbit.communicator.test;


import java.util.Random;

import org.protorabbit.communicator.BaseJSONHandler;
import org.protorabbit.communicator.Namespace;

@Namespace("/secure")
// test with a URL :
// good: http://localhost:8080/protorabbit/secure/testNamespace!doFoo.action?name=5.2&json={blah:1}
// bad : http://localhost:8080/protorabbit/secure2/testNamespace!doFoo.action?name=5.2&json={blah:1}
public class TestPollerHandler extends BaseJSONHandler{
    
    private String name = "not set";

    public void setJson(String name) {
       // System.out.println("we got a set with " + name);
        this.name = name;
    }

    public void setName(double name) {
      //  System.out.println("we got a long set with " + name);
    }

    public String doFoo() {
        setIsPoller( true );
        setModel ( "we set the name to " + name );
        Random r = new Random();
        int rand = r.nextInt(500);
        if (rand == 1 ) {
            addActionError("Bad things happen. We are error #3.");
        } else if ( rand == 2 ) {
            addActionError("We are a bad error #4.");
        }
        try {
            Thread.currentThread().sleep(12);
        } catch (InterruptedException e) {
        }
        return SUCCESS;
    }
}
