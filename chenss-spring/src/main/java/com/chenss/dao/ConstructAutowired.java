package com.chenss.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConstructAutowired {
	public ConstructAutowired() {
		System.out.println("ConstructAutowired default construct");
	}
	@Autowired
	public ConstructAutowired(UserDao userDao) {
		userDao.query();
		System.out.println("ConstructAutowired userDao construct;  " + userDao.hashCode());
	}
}
