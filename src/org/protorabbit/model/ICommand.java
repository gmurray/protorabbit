package org.protorabbit.model;

import java.io.IOException;
import java.io.OutputStream;

public interface ICommand {
	
	public static int UNKNOWN = 0;	
	public static int INSERT = 1;
	public static int INCLUDE = 2;

    public void setContext(IContext ctx);
	public int getStartIndex();
	public int getEndIndex();
	public int getType();
	public String[] getParams();
	public void doProcess(OutputStream os) throws IOException;
}
