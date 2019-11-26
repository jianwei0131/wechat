package com.spiderclould.api;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.spiderclould.entity.Result;
import com.spiderclould.service.Wechat;

@RestController
public class WxApi {
	
	private final static Logger logger = LoggerFactory.getLogger(WxApi.class);
	
	@Value("${wechat.conf.path}")
	private String wechatConfPath;
	
	@PostMapping("/message")
	public Result<Object> message(@RequestBody Map<String, Object> params) {
		logger.info("Received parameters {}", params);
//		Wechat.getInstance().transferMessage(deviceType, deviceId, openid, content);
		return new Result(params);
	}
	

}
