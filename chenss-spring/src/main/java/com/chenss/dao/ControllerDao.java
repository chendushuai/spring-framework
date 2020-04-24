package com.chenss.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ControllerDao {

	@Autowired
	public ServiceDao serviceDao;
	public ControllerDao() {
		System.out.println("初始化ControllerDao");
	}
}
