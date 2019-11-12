package com.chenss.processor;

import com.chenss.dao.NoAnnotationDao;
import com.chenss.dao.TaggerDao;
import com.chenss.dao.UserDao;
import com.chenss.dao.UserDaoImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.stereotype.Component;

@Component
public class ChenssBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) beanFactory;
		// 手动注册一个Bean定义
		GenericBeanDefinition genericBeanDefinition = new GenericBeanDefinition();
		genericBeanDefinition.setBeanClass(NoAnnotationDao.class);
		// 该句话用于进行手动给定参数值后的构造方法选择过程测试
		//genericBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue(defaultListableBeanFactory.getBean("userDaoImpl2"));
		// 用于指定构造函数的选择方式为自动装入
		//genericBeanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
		// 提供Supplier的函数来创建对象
		//genericBeanDefinition.setInstanceSupplier(() -> new NoAnnotationDao((UserDao) defaultListableBeanFactory.getBean("userDaoImpl2")));
		defaultListableBeanFactory.registerBeanDefinition("noAnnotationDao",genericBeanDefinition);

		// 修改已经注册完成的bean定义
		//BeanDefinition existBeanDefinition = defaultListableBeanFactory.getBeanDefinition("userDao");
		//existBeanDefinition.setBeanClassName(TaggerDao.class.getName());
	}
}
