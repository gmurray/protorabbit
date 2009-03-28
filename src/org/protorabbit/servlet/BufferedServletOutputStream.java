package org.protorabbit.servlet;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import javax.servlet.ServletOutputStream;

public class BufferedServletOutputStream extends ServletOutputStream {

	ByteArrayOutputStream buffer = null;

	BufferedServletOutputStream(ByteArrayOutputStream buffer) {
		this.buffer = buffer;
	}

	public void write(int b) throws IOException {
		this.buffer.write(b);
	}

	public void write(byte[] b) throws IOException {
		new Throwable().printStackTrace();
		this.buffer.write(b);
	}

	public void write(byte[] b, int off, int len) throws IOException {
		new Throwable().printStackTrace();
		this.buffer.write(b, off, len);
	}

	public void flush() throws IOException {
		this.buffer.flush();
	}

	public void close() throws IOException {
		this.buffer.close();
	}
}
