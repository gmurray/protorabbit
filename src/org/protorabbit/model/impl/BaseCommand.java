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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.protorabbit.Config;
import org.protorabbit.model.ICommand;
import org.protorabbit.model.IContext;
import org.protorabbit.model.IParameter;
import org.protorabbit.model.IDocumentContext;

public abstract class BaseCommand implements ICommand {

    protected int start = -1;
    protected int end = -1;
    protected int commandType = ICommand.UNKNOWN;
    protected int commandIndex = -1;

    protected IParameter[] params = null;

    protected int processOrder = ICommand.PROCESS_DEFAULT;

    private IDocumentContext document;

    protected String uuid = null;
    /*
    private static class LocalContext {
        public ByteArrayOutputStream buffer;
        public IContext ctx;

    }

    private ThreadLocal<LocalContext> localContext = new ThreadLocal<LocalContext>() {
        protected LocalContext initialValue() {
            LocalContext lc = new LocalContext();
            lc.buffer = new ByteArrayOutputStream();
            return lc;
        }
    };*/

    public BaseCommand() {
        uuid = Config.generateUUId();
    }

    public String getUUId() {
        return uuid;
    }

    public void reset() {
       // localContext.remove();
    }

    public ByteArrayOutputStream getBuffer(IContext ctx) {
        if (ctx.getBuffer(getUUId()) == null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ctx.setBuffer(getUUId(), bos );
            return bos;
        } else {
            return ctx.getBuffer(getUUId());
        }
    }

    public void setDocumentContext(IDocumentContext document) {
         this.document = document;
    }

    public void setProcessOrder(int processOrder) {
        this.processOrder = processOrder;
    }

    public int getProcessOrder() {
        return processOrder;
    }

    public abstract void doProcess(IContext ctx) throws IOException;

 //   public void setContext(IContext ctx) {
 //       localContext.get().ctx = ctx;
  //  }

 //   public IContext getContext() {
 //       return localContext.get().ctx;
 //   }

    public IDocumentContext getDocumentContext() {
      return document;
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
        this.commandType = type;
    }

}
