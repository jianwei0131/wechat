package com.spiderclould.holder;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;

/**
 * 
 * @author David Wang
 * 2017-09-14
 */
public class EnvironmentHolder {

	public static String get(String key) {
		Environment env = WebContextHolder.getBean(Environment.class);
		return env.getProperty(key);
	}
	
	public static String get(String key, String defaultVal) {
		String value = get(key);
		return StringUtils.isNotBlank(value)?value:defaultVal;
	}
	
	public static <T> T get(String key, Class<T> clazz) {
		Environment env = WebContextHolder.getBean(Environment.class);
		return env.getProperty(key, clazz);
	}
}
