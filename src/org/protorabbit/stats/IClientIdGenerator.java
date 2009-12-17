package org.protorabbit.stats;

import javax.servlet.http.HttpServletRequest;

public interface IClientIdGenerator {
    public String getClientId( HttpServletRequest r);
 }
