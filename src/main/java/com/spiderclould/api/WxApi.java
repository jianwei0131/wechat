package com.spiderclould.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spiderclould.entity.Result;
import com.spiderclould.service.Wechat;
import com.spiderclould.util.ResponseUtil;

@RestController
public class WxApi {
	
	private final static Logger logger = LoggerFactory.getLogger(WxApi.class);
	
	@Value("${wechat.conf.path}")
	private String wechatConfPath;
	
	private Wechat wechat = Wechat.getInstance();
	
//	@RequestMapping("/message")
//	public Result<Object> message(@RequestParam(required = false) Map<String, Object> formParams, @RequestBody(required = false) Map<String, Object> bodyParams, HttpServletRequest request, HttpServletResponse response) throws IOException {
//		Map<String, Object> params = new TreeMap<String, Object>();
//		if(formParams != null && !formParams.isEmpty()) {
//			params.putAll(formParams);
//		}
//		if(bodyParams != null && !bodyParams.isEmpty()) {
//			params.putAll(bodyParams);
//		}
//		logger.info("Received parameters {}", params);
//		String echostr = (String)params.get("echostr");
//		String nonce = (String)params.get("nonce");
//		String signature = (String)params.get("signature");
//		String timestamp = (String)params.get("timestamp");
//		if(StringUtils.isNotBlank(echostr)) {
//			ResponseUtil.printStringToResponse(echostr, response);
//			return null;
//		}
//		wechat.getAutoreply();
//		return new Result<>("success");
//	}
	
	@RequestMapping("/message")
	public void message( HttpServletRequest request, HttpServletResponse response) throws IOException {
		Map<String, Object> params = new TreeMap<String, Object>();
		Map<String, String[]> formParams = request.getParameterMap();
		if(formParams != null && !formParams.isEmpty()) {
			formParams.forEach((k,v) -> params.put(k, v[0]));
			logger.info("Received parameters {}", params);
			
			String signature = (String)params.get("signature");
			String timestamp = (String)params.get("timestamp");
			String nonce = (String)params.get("nonce");
			String openid = (String)params.get("openid");
			
			
			
			String echostr = (String)params.get("echostr");
			if(StringUtils.isNotBlank(echostr)) {
				ResponseUtil.printStringToResponse(echostr, response);
				return ;
			}
		}
		
		if(request.getReader().ready()){
			try(BufferedReader reader = request.getReader();){
				reader.lines().forEach(str -> System.out.println(str));
				
			}
		}
		
//		wechat.getAutoreply();
		ResponseUtil.printStringToResponse("success", response);
	}
	

}
