package com.chenss.processor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

@Component
public class ChenssBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) beanFactory.getBeanDefinition("userDao");
		annotatedBeanDefinition.setScope("prototype");
	}
}
