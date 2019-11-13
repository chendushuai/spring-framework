package com.chenss.dao;

import org.springframework.stereotype.Component;

@Component
public class TaggerDao {
	public TaggerDao(UserDao userDao) {
		System.out.println("TaggerDao UserDao");
	}

	public TaggerDao(UserDaoImpl userDao) {
		System.out.println("TaggerDao UserDaoImpl");
	}
}
