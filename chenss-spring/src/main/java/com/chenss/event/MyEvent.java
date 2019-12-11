package com.chenss.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Component;

public class MyEvent extends ApplicationEvent {
	/**
	 * Create a new {@code ApplicationEvent}.
	 *
	 * @param source 事件最初发生的对象或与事件相关联的对象(绝不是{@code null})
	 */
	public MyEvent(Object source,int fileSize,int readSize) {
		super(source);
		this.fileSize=fileSize;
		this.readSize=readSize;
	}

	private int fileSize;
	private int readSize;

	public int getFileSize() {
		return fileSize;
	}

	public void setFileSize(int fileSize) {
		this.fileSize = fileSize;
	}

	public int getReadSize() {
		return readSize;
	}

	public void setReadSize(int readSize) {
		this.readSize = readSize;
	}
}
