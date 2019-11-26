package com.spiderclould.holder;

import java.io.Writer;

/**
 * 
 * @author David Wang
 * 2017-10-18
 */
public class ResponseWriterHolder {
	private final static ThreadLocal<Writer> WRITER = new ThreadLocal<>();
	
	public static void setWriter(Writer writer) {
		WRITER.set(writer);
	}
	
	public static Writer getWriter() {
		return WRITER.get();
	}
	
	public static void clear() {
		WRITER.remove();
	}
}
