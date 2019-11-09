package com.chenss.processor;

import com.chenss.dao.UserDao;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class CustomAopBeanPostProcessor implements BeanPostProcessor {

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof UserDao) {
			bean = CglibUtil.getProxy();
		}
		return bean;
	}
}
