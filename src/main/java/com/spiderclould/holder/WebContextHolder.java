package com.spiderclould.holder;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import com.spiderclould.service.Wechat;

/**
 * 
 * @author David Wang
 * 2017年7月20日
 */
@Service
public class WebContextHolder implements ApplicationContextAware  {
	
	private static ApplicationContext applicationContext;
	private static boolean isReady = false;

	@Value("${wechat.conf.path}")
	private String wechatConfPath;
	
	@SuppressWarnings("unchecked")
	public static <T> T getBean(String name){
		return (T)applicationContext.getBean(name);
	}
	
	public static <T> T getBean(Class<T> clazz){
		return (T)applicationContext.getBean(clazz);
	}
	
	public static <T> void setBean(Class<T> clazz){
		 T t = applicationContext.getBean(clazz);
		
	}
	
	@Override
	public void setApplicationContext(ApplicationContext arg0)
			throws BeansException {
		applicationContext = arg0;
		isReady = true;
		Wechat.createInstance(wechatConfPath);
	}
	
	public static boolean isReady() {
		return isReady;
	}

}
