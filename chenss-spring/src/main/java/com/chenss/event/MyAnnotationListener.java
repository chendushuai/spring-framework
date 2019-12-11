package com.chenss.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MyAnnotationListener {
	@EventListener(MyEvent.class)
	public void listen(MyEvent event) {
		if (event.getReadSize()==event.getFileSize()) {
			System.out.println("MyAnnotationListener文件上传，文件上传完成  "+event.getFileSize()+"  "+event.getReadSize());
		}
	}
}
