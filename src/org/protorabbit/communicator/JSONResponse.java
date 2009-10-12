package org.protorabbit.communicator;

import java.util.Collection;
/*
 * A response for a JSON request
*/
public class JSONResponse {

    private Object data = null;
    private Collection<String> errors = null;
    private Long pollInterval = null;
    private String result = null;

    public JSONResponse() {}

    public Long getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(Long pollInterval) {
        this.pollInterval = pollInterval;
    }

    public  Collection<String> getErrors() {
        return errors;
    }

    public void setErrors(Collection<String> errors) {
        this.errors = errors;
    }

    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

}
