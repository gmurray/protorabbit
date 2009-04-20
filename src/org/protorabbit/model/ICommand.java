package org.protorabbit.model;

import java.io.IOException;
import java.io.OutputStream;

public interface ICommand {
	
	public static int UNKNOWN = 0;
	public static int INSERT = 1;
	public static int INCLUDE = 2;
	public static int INCLUDE_RESOURCES = 3;
	public static int CUSTOM = 4;	
	
    public void setContext(IContext ctx);
	public int getStartIndex();
	public void setStartIndex(int index);
	public int getEndIndex();
	public void setEndIndex(int index);
	public int getType();
	public void setType(int type);
	public String[] getParams();
	public void setParams(String[] params);
	public void doProcess(OutputStream os) throws IOException;
}
