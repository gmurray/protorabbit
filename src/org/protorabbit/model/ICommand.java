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

package org.protorabbit.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public interface ICommand {

    public static int UNKNOWN = 0;
    public static int INSERT = 1;
    public static int INCLUDE = 2;
    public static int INCLUDE_RESOURCES = 3;
    public static int CUSTOM = 4;

    public static int PROCESS_DEFAULT =5;
    public static int PROCESS_LAST = 6;
    public static int PROCESS_FIRST = 7;

    public IDocumentContext getDocumentContext();
    public void setDocumentContext(IDocumentContext document);
    public ByteArrayOutputStream getBuffer();
    public void setBuffer(ByteArrayOutputStream buffer);
    public void setProcessOrder(int order);
    public int getProcessOrder();
    public void setContext(IContext ctx);
    public int getStartIndex();
    public void setStartIndex(int index);
    public int getEndIndex();
    public void setEndIndex(int index);
    public int getType();
    public void setType(int type);
    public void reset();
    public IParameter[] getParams();
    public void setParams(IParameter[] params);
    public void doProcess() throws IOException;
}
