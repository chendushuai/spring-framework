package com.chenss.test;

import com.chenss.config.Appconfig;
import com.chenss.dao.AopAspectJDao;
import com.chenss.dao.AopPrototypeAspectJDao;
import com.chenss.dao.ChenssFactoryBean;
import com.chenss.dao.UserDao;
import com.chenss.event.MyEvent;
import com.chenss.processor.ChenssRegisterBeanFactoryPostProcessor;
import com.chenss.utils.FileUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class Test {
	public static void main(String[] args) {
//		BeanFactory beanFactory = new ClassPathXmlApplicationContext("");

		//bean标签、@Service等标签、JavaConfig的@bean标签
		// C01 初始化注解配置上下文对象
		System.out.println("C01 初始化注解配置上下文对象");
		AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();
		// C02 注册配置类
		annotationConfigApplicationContext.register(Appconfig.class);
		// C02_1 添加自定义bean工厂后置处理器
		annotationConfigApplicationContext.addBeanFactoryPostProcessor(new ChenssRegisterBeanFactoryPostProcessor());
		// C03 刷新上下文对象
		annotationConfigApplicationContext.refresh();
		//annotationConfigApplicationContext.start();
		UserDao userDao = (UserDao) annotationConfigApplicationContext.getBean("userDao");
		UserDao userDao1 = (UserDao) annotationConfigApplicationContext.getBean("userDao");
		System.out.println(userDao.getClass().getSimpleName() + "  " + userDao.hashCode());
		System.out.println(userDao1.getClass().getSimpleName() + "  " + userDao1.hashCode());

		UserDao userDaoImpl2 = (UserDao) annotationConfigApplicationContext.getBean("userDaoImpl2");
		System.out.println(userDaoImpl2.getClass().getSimpleName() + "  " + userDaoImpl2.hashCode());

		AopAspectJDao aopAspectJDao = (AopAspectJDao) annotationConfigApplicationContext.getBean("aopAspectJDao");
		aopAspectJDao.print("aopTest1");
		aopAspectJDao = (AopAspectJDao) annotationConfigApplicationContext.getBean("aopAspectJDao");
		aopAspectJDao.print("aopTest2");

		AopPrototypeAspectJDao aopPrototypeAspectJDao = (AopPrototypeAspectJDao) annotationConfigApplicationContext.getBean("aopPrototypeAspectJDao");
		aopPrototypeAspectJDao.print("aopTest1");
		aopPrototypeAspectJDao = (AopPrototypeAspectJDao) annotationConfigApplicationContext.getBean("aopPrototypeAspectJDao");
		aopPrototypeAspectJDao.print("aopTest2");


		/*try {
			File fileIn = new File("e://user.txt");
			File fileOut = new File("e://userOut.txt");
			if (!fileOut.exists()) {
				fileOut.createNewFile();
			}
			FileUtils.fileUploadWrite(new FileInputStream(fileIn), new FileOutputStream(fileOut), annotationConfigApplicationContext);
		}catch (Exception ex) {

		}*/
	}
}
