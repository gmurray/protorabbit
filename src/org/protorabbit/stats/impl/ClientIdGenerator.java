package org.protorabbit.stats.impl;

import javax.servlet.http.HttpServletRequest;

import org.protorabbit.stats.IClientIdGenerator;

public class ClientIdGenerator implements IClientIdGenerator {

    public String getClientId(HttpServletRequest r) {
        return r.getRemoteAddr();
    }

}
