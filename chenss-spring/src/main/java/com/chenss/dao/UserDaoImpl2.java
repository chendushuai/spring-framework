package com.chenss.dao;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class UserDaoImpl2 implements UserDao{
	@Override
	public void query() {
		System.out.println("UserDaoImpl2");
	}
}
