package com.example.voiceassistant;

public class ListData {
	
	public static final int SEND = 1;      // 发送
	public static final int RECEIVER = 2;  // 接收
	private String content;
	// 标识，判断是左边，还是右边。
	private int flag;    
	private String time;
	
	public ListData(String content,int flag,String time) {
		setContent(content);
		setFlag(flag);
		setTime(time);
	}
	
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public int getFlag() {
		return flag;
	}
	public void setFlag(int flag) {
		this.flag = flag;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
}

