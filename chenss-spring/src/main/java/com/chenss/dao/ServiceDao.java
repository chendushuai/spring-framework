package com.chenss.dao;

import org.springframework.stereotype.Component;

@Component
public class ServiceDao {
	private ControllerDao controllerDao;
	public ServiceDao(ControllerDao controllerDao) {
		this.controllerDao=controllerDao;
		System.out.println("初始化ServiceDao");
	}
}
