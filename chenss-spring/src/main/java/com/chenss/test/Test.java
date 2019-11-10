package com.chenss.test;

import com.chenss.config.Appconfig;
import com.chenss.dao.AopAspectJDao;
import com.chenss.dao.AopPrototypeAspectJDao;
import com.chenss.dao.ChenssFactoryBean;
import com.chenss.dao.UserDao;
import com.chenss.processor.ChenssRegisterBeanFactoryPostProcessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Test {
	public static void main(String[] args) {
//		BeanFactory beanFactory = new ClassPathXmlApplicationContext("");

		//bean标签、@Service等标签、JavaConfig的@bean标签
		AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();
		annotationConfigApplicationContext.register(Appconfig.class);
		annotationConfigApplicationContext.addBeanFactoryPostProcessor(new ChenssRegisterBeanFactoryPostProcessor());
		annotationConfigApplicationContext.refresh();
		//annotationConfigApplicationContext.start();
		UserDao userDao = (UserDao) annotationConfigApplicationContext.getBean("userDao");
		UserDao userDao1 = (UserDao) annotationConfigApplicationContext.getBean("userDao");
		System.out.println(userDao);
		System.out.println(userDao1);

		AopAspectJDao aopAspectJDao = (AopAspectJDao) annotationConfigApplicationContext.getBean("aopAspectJDao");
		aopAspectJDao.print("aopTest1");
		aopAspectJDao = (AopAspectJDao) annotationConfigApplicationContext.getBean("aopAspectJDao");
		aopAspectJDao.print("aopTest2");

		AopPrototypeAspectJDao aopPrototypeAspectJDao = (AopPrototypeAspectJDao) annotationConfigApplicationContext.getBean("aopPrototypeAspectJDao");
		aopPrototypeAspectJDao.print("aopTest1");
		aopPrototypeAspectJDao = (AopPrototypeAspectJDao) annotationConfigApplicationContext.getBean("aopPrototypeAspectJDao");
		aopPrototypeAspectJDao.print("aopTest2");
	}
}
