package com.chenss.dao;

public class NoAnnotationDao {
	public NoAnnotationDao() {
		System.out.println("NoAnnotationDao Init");
	}
	public NoAnnotationDao(UserDao userDao) {
		userDao.query();
		System.out.println("NoAnnotationDao UserDao。 hashcode: "+userDao.hashCode());
	}
}
