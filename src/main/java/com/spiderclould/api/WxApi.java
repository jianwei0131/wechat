package com.spiderclould.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spiderclould.entity.Message;
import com.spiderclould.service.Wechat;
import com.spiderclould.util.ResponseUtil;

@RestController
public class WxApi {

	private final static Logger logger = LoggerFactory.getLogger(WxApi.class);

	@Value("${wechat.conf.path}")
	private String wechatConfPath;

	@RequestMapping("/message")
	public void message(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.info("Received message: ");
		Map<String, Object> params = new TreeMap<String, Object>();
		Map<String, String[]> formParams = request.getParameterMap();
		if (formParams != null && !formParams.isEmpty()) {
			formParams.forEach((k, v) -> params.put(k, v[0]));
			logger.info("Received parameters {}", params);

			String signature = (String) params.get("signature");
			String timestamp = (String) params.get("timestamp");
			String nonce = (String) params.get("nonce");
			String openid = (String) params.get("openid");

			String echostr = (String) params.get("echostr");
			if (StringUtils.isNotBlank(echostr)) {
				ResponseUtil.printStringToResponse(echostr, response);
				return;
			}
		}

		try (InputStream is = request.getInputStream();) {
			if (is.available() > 0) {
				Message msg = new Message(is);

				logger.info("toUser: {}", msg.getToUser());
				logger.info("fromUserName: {}", msg.getFromUserName());
				logger.info("createTime: {}", msg.getCreateTime());
				logger.info("msgType: {}", msg.getMsgType());
				logger.info("content: {}", msg.getContent());
				logger.info("msgId: {}", msg.getMsgId());
				
				if(msg.getMsgType().equals("text")) {
					Message replyMsg = new Message();
					replyMsg.setToUser(msg.getFromUserName());
					replyMsg.setFromUserName(msg.getToUser());
					replyMsg.setMsgType("text");
					replyMsg.setContent("已收到: "+ msg.getContent());
					replyMsg.setCreateTime(String.valueOf(new Date().getTime()));
					ResponseUtil.printStringToResponse(replyMsg, response);
					return;
				}else {
					Wechat.getInstance().getAutoreply();
				}
			}
		} catch (Exception e) {
			logger.error("Exception occurred.", e);
		}

		ResponseUtil.printStringToResponse("success", response);
	}

}
