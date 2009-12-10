package org.protorabbit.json;

public class TestObject {

    enum Foo {One, Two, Three, Four};

    String firstName = null;
    String lastName = null;
    Long timeout = null;
    Foo foo = null;


    public String getFirstName() {
        return firstName;
    }
    
    public void setFoo(Foo foo) {
        this.foo = foo;
    }
    
    public Foo getFoo() {
        return foo;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public TestObject() {}
    
    public String toString() {
        return " {" +
               "firstName=" + firstName + "," +
               "lastName=" + lastName + "," +
               " foo=" + foo + "," + 
               "timeout=" + timeout + "}";
        
    }
}
