package com.spiderclould.entity;

import java.io.InputStream;

import com.spiderclould.util.XmlHelper;

public class Message {
	String toUser;
	String fromUserName;
	String createTime;
	String msgType;
	String content;
	String msgId;

	public Message() {

	}

	public Message(InputStream is) {
		XmlHelper xml = new XmlHelper(is);
		toUser = xml.getSimpleStringValue("ToUserName");
		fromUserName = xml.getSimpleStringValue("FromUserName");
		createTime = xml.getSimpleStringValue("CreateTime");
		msgType = xml.getSimpleStringValue("MsgType");
		if(msgType.equals("text")) {
			content = xml.getSimpleStringValue("Content");
		}
		msgId = xml.getSimpleStringValue("MsgId");
	}

	public String getXml() {
		StringBuilder b = new StringBuilder();
		b.append("<xml>");
		b.append("<ToUserName><![CDATA[").append(toUser).append("]]></ToUserName>");
		b.append("<FromUserName><![CDATA[").append(fromUserName).append("]]></FromUserName>");
		b.append("<CreateTime>").append(createTime).append("</CreateTime>");
		b.append("<MsgType><![CDATA[").append(msgType).append("]]></MsgType>");
		b.append("<Content><![CDATA[").append(content).append("]]></Content>");
		b.append("</xml>");

		return b.toString();
	}

	public String getToUser() {
		return toUser;
	}

	public String getFromUserName() {
		return fromUserName;
	}

	public String getCreateTime() {
		return createTime;
	}

	public String getMsgType() {
		return msgType;
	}

	public String getContent() {
		return content;
	}

	public String getMsgId() {
		return msgId;
	}

	public void setToUser(String toUser) {
		this.toUser = toUser;
	}

	public void setFromUserName(String fromUserName) {
		this.fromUserName = fromUserName;
	}

	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}

	public void setMsgType(String msgType) {
		this.msgType = msgType;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}

}
