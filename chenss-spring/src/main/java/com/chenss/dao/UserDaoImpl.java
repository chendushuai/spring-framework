package com.chenss.dao;

import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;

@Repository("userDao")
public class UserDaoImpl implements UserDao {
	public UserDaoImpl() {
		System.out.println("userDao create");
	}
	@PostConstruct
	public void init() {
		System.out.println("userDao init");
	}
	@Override
	public void query() {
		System.out.println("UserDaoImpl");
	}
}
