package com.spiderclould.util;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSON;
import com.spiderclould.holder.ResponseWriterHolder;

public class ResponseUtil {


	public static void printStringToResponse(String string, HttpServletResponse response) throws IOException {
		try {
			Writer r = ResponseWriterHolder.getWriter();
			if(r == null) {
				response.setCharacterEncoding("UTF-8");
				response.setContentType("text/html;charset=UTF-8");
				r = response.getWriter();
				ResponseWriterHolder.setWriter(r);
			}
			r.write(string);
			r.flush();
			r.close();
			
		}finally {
			ResponseWriterHolder.clear();
		}
	}
	
	public static void printStringToResponse(Object object, HttpServletResponse response) throws IOException {
		String jsonStr = JSON.toJSONString(object);
		printStringToResponse(jsonStr, response);
	}
}
