package com.chenss.processor;

import com.chenss.dao.NoAnnotationDao;
import com.chenss.dao.TaggerDao;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
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
		defaultListableBeanFactory.registerBeanDefinition("noAnnotationDao",genericBeanDefinition);

		// 修改已经注册完成的bean定义
		BeanDefinition existBeanDefinition = defaultListableBeanFactory.getBeanDefinition("userDao");
		existBeanDefinition.setBeanClassName(TaggerDao.class.getName());
	}
}
