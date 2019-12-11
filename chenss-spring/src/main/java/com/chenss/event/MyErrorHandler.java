package com.chenss.event;

import org.springframework.util.ErrorHandler;

public class MyErrorHandler implements ErrorHandler {
	@Override
	public void handleError(Throwable t) {
		System.out.println("发生了异常，异常内容为："+t.getStackTrace());
	}
}
