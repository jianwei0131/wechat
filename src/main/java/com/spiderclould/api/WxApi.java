package com.spiderclould.api;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.spiderclould.entity.Result;

@RestController
public class WxApi {
	
	@PostMapping("/message")
	public Result<Object> message(@RequestBody Map<String, Object> params) {
		
		
		return new Result(params);
	}
	

}
