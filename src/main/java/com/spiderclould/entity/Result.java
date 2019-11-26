package com.spiderclould.entity;

public class Result <T> {

	private String code;
	private String msg;
	private T data;
	
	public Result(T data) {
		this.code = "200";
		this.msg = "success.";
		this.data = data;
	}
	public Result(String code, String msg) {
		this.code = code;
		this.msg = msg;
	}
	
	
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	public T getData() {
		return data;
	}
	public void setData(T data) {
		this.data = data;
	}
	
}
