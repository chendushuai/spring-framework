package com.chenss.dao;

import org.springframework.beans.factory.FactoryBean;

public class ChenssFactoryBean implements FactoryBean {
	@Override
	public Object getObject() throws Exception {
		return new UserDaoImpl();
	}

	@Override
	public Class<?> getObjectType() {
		return UserDaoImpl.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
