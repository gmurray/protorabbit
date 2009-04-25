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

package org.protorabbit.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class BufferedServletResponse extends HttpServletResponseWrapper {

	private ByteArrayOutputStream bos;
	private PrintWriter writer;

	public BufferedServletResponse(ServletResponse response) {
		super((HttpServletResponse) response);
	}

	public BufferedServletResponse(ServletResponse response,
			ByteArrayOutputStream bos) {
		super((HttpServletResponse) response);
		this.bos = bos;
		writer = new PrintWriter(new OutputStreamWriter(bos));

	}

	public BufferedServletResponse(HttpServletResponse res) {
		super(res);
		this.bos = new ByteArrayOutputStream();
	}

	public ServletOutputStream getOutputStream() throws IOException {
		return new BufferedServletOutputStream(this.bos);
	}

	public PrintWriter getWriter() throws IOException {
		return writer;
	}

	public void flushBuffer() throws IOException {
		this.writer.flush();
		super.flushBuffer();
	}

	public String toString() {
		return this.bos.toString();
	}

	public byte[] getByteArray() {
		return this.bos.toString().getBytes();
	}

	public char[] getCharArray() {
		return this.bos.toString().toCharArray();
	}

	public void close() {
		try {
			this.bos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
