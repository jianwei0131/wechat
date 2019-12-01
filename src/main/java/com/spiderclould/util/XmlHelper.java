package com.spiderclould.util;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class XmlHelper {
	private final static Logger logger = LoggerFactory.getLogger(XmlHelper.class);
	private Document dom;
	
	public XmlHelper(InputStream is) {
		try{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();  
			DocumentBuilder builder = factory.newDocumentBuilder();  
			dom = builder.parse(is);
		} catch (Exception e) {
			logger.error("",e);
		} 
	}
	
	public Document getDom() {
		return dom;
	}
	
	public String getSimpleStringValue(String key) {
		return dom.getElementsByTagName(key).item(0).getTextContent();
	}
	
	
}
