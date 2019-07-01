package com.chenss.test;

import com.chenss.config.Appconfig;
import com.chenss.dao.ChenssFactoryBean;
import com.chenss.dao.UserDao;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Test {
	public static void main(String[] args) {
//		BeanFactory beanFactory = new ClassPathXmlApplicationContext("");

		//bean标签、@Service等标签、JavaConfig的@bean标签
		AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();
		annotationConfigApplicationContext.register(Appconfig.class);
		annotationConfigApplicationContext.refresh();
		//annotationConfigApplicationContext.start();
		UserDao userDao = (UserDao) annotationConfigApplicationContext.getBean("userDao");
		System.out.println(userDao);
//
//		annotationConfigApplicationContext.scan("");
	}
}
