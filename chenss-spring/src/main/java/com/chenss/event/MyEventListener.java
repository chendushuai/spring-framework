package com.chenss.event;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class MyEventListener implements ApplicationListener<MyEvent> {
	@Override
	public void onApplicationEvent(MyEvent event) {
		double fileSize = ((double) event.getFileSize());
		double readSize = ((double) event.getReadSize());
		double percent = readSize / fileSize *100;
		System.out.println(String.format("MyEventListener监控，当前文件上传百分比：{%s}",percent));
	}
}
