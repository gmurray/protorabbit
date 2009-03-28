package org.protorabbit.model.impl;

import java.io.IOException;
import java.io.OutputStream;

import org.protorabbit.model.ICommand;
import org.protorabbit.model.IContext;

public abstract class BaseCommand implements ICommand {
	
	int start = -1;
	int end = -1;
	int commandType = -1;
	
	IContext ctx = null;
	
	String[] params = null;
	
	public BaseCommand(int commandType, int start, int end, String... params) {
		this.start = start;
		this.end = end;
		this.commandType = commandType;
		this.params = params;
	}

	public abstract void doProcess(OutputStream out) throws IOException;

	public void setContext(IContext ctx) {
		this.ctx = ctx;
	}
	
	public String[] getParams() {
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

}
