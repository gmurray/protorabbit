package org.protorabbit.communicator.test;

import java.util.Random;

import org.protorabbit.communicator.BaseJSONHandler;

// test with a URL :
//  http://localhost:8080/protorabbit/secure/test!doFoo.action?name=5.2&json={blah:1}
public class TestHandler extends BaseJSONHandler{

    private String name = "not set";

    public void setJson(String name) {
  //      System.out.println("we got a setJson with " + name);
        this.name = name;
    }

    public void setName(String name) {
     //   System.out.println("we got a setName string set with " + name);	
    }

    public void setName(long name) {
        System.out.println("we got a setName long set with " + name);
    }
    
    public String doFoo() {
        setModel( "nam set to " + name );
        Random r = new Random();
        int rand = r.nextInt(500);
        if (rand == 1 ) {
            addActionError("Bad things happen. We are error #1.");
        } else if ( rand == 2 ) {
            addActionError("We are a bad error #2.");
        }
        try {
            Thread.currentThread().sleep(2);
        } catch (InterruptedException e) {
        }
        return JSON;
    }
}
