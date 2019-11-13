package com.chenss.test;

import com.chenss.dao.UserDao;
import com.chenss.dao.UserDaoImpl;
import org.springframework.util.ClassUtils;

public class AssignableValueMethodTest {
	public static void main(String[] args) {
		UserDaoImpl userDao = new UserDaoImpl();
		System.out.println(ClassUtils.isAssignableValue(UserDaoImpl.class,userDao));
		System.out.println(ClassUtils.isAssignableValue(UserDao.class,userDao));
		System.out.println(ClassUtils.isAssignableValue(Object.class,userDao));
		System.out.println(ClassUtils.isAssignable(UserDaoImpl.class,UserDaoImpl.class));
		System.out.println(ClassUtils.isAssignable(UserDao.class,UserDaoImpl.class));
		System.out.println(ClassUtils.isAssignable(Object.class,UserDaoImpl.class));
	}
}
