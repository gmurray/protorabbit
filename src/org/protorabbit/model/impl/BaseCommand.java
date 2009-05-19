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

package org.protorabbit.model.impl;

import java.io.IOException;
import java.io.OutputStream;

import org.protorabbit.model.ICommand;
import org.protorabbit.model.IContext;
import org.protorabbit.model.IParameter;

public abstract class BaseCommand implements ICommand {

    int start = -1;
    int end = -1;
    int commandType = -1;
    int type = ICommand.UNKNOWN;

    protected IContext ctx = null;

    protected IParameter[] params = null;

    public BaseCommand() {
    }

    public abstract void doProcess(OutputStream out) throws IOException;

    public void setContext(IContext ctx) {
        this.ctx = ctx;
    }

    public IParameter[] getParams() {
        return params;
    }

    public int getType() {
        return commandType;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

    public String toString() {
        return "Command : { start = " + start + ", end=" + end + ", commandType=" + commandType + ", params=" + params[0] + "}";
    }

    public void setEndIndex(int index) {
        this.end = index;
        
    }

    public void setParams(IParameter[] params) {
        this.params = params;
        
    }

    public void setStartIndex(int index) {
        this.start = index;
    }

    public void setType(int type) {
        this.type = type;
    }

}
