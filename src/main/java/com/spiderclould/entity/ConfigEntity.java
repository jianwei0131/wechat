package com.spiderclould.entity;

import java.io.*;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

public class ConfigEntity {

    private String appid = null;

    private String appsecret = null;

    private Properties prop = new Properties();
   
    private String filepath;
    
    public ConfigEntity(String filepath) {
    	this.filepath = filepath;
    	File file = new File(this.filepath);
    	
    	BufferedReader br = null;
    	try {
    		br = new BufferedReader(new FileReader(file));
    		prop.load(br);
    		appid = prop.getProperty("appid", "");
    		appsecret = prop.getProperty("appsecret", "");
    	} catch (FileNotFoundException e) {
    		e.printStackTrace();
    	} catch (IOException e) {
    		e.printStackTrace();
    	}finally {
    		try {
    			br.close();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    	}
    }
    
    public String getAppid() {
		return appid;
	}

	public String getAppsecret() {
		return appsecret;
	}

	public String getAccessToken() {
    	return prop.getProperty("access_token", null);
    }

	public Long getExpiresIn() {
		String expires = prop.getProperty("expires_in", null);
		if(StringUtils.isBlank(expires)) {
			return Long.valueOf("0");
		}
		return Long.valueOf(expires);
	}

	public void setAccessToken(AccessToken token) {
		File file = new File(this.filepath);
    	
    	BufferedWriter bw = null;
    	try {
    		bw = new BufferedWriter(new FileWriter(file));
    		prop.setProperty("access_token", token.getAccessToken());
    		prop.setProperty("expires_in", String.valueOf(token.getExpireTime()));
    		prop.store(bw, "");
    	} catch (FileNotFoundException e) {
    		e.printStackTrace();
    	} catch (IOException e) {
    		e.printStackTrace();
    	}finally {
    		try {
    			bw.close();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    	}
	}

	
}
