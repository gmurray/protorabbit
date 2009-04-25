/*
 * Protorabbit
 *
 * Copyright (c) 2009 Greg Murray (protorabbit.org)
 * 
 * Licensed under the MIT License:
 * 
 *  http://www.opensource.org/licenses/mit-license.php
 *
 */

package org.protorabbit.accelerator;

public interface ICallback {

    public int getStatus();
    public void setStatus(int status);
    public String getContentType();
    public StringBuffer getContent();
    public void setContent(StringBuffer content);
    public void execute();
}
