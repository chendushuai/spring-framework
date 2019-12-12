package com.chenss.config;

import com.chenss.dao.BeanTestA;
import com.chenss.dao.BeanTestB;
import com.chenss.event.MyErrorHandler;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.*;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@Component
@ComponentScan("com.chenss")
@EnableAspectJAutoProxy
//@ImportResource("classpath:spring.xml")
public class Appconfig {
	@Bean
	public BeanTestA getBeanTestA() {
		return new BeanTestA();
	}
	@Bean
	public BeanTestB getBeanTestB() {
		getBeanTestA();
		return new BeanTestB();
	}

	@Bean
	public ThreadPoolTaskExecutor poolTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5);
		executor.setMaxPoolSize(30);
		executor.setQueueCapacity(100);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
		executor.initialize();
		return executor;
	}

	@Bean("applicationEventMulticaster")
	public SimpleApplicationEventMulticaster simpleApplicationEventMulticaster(BeanFactory beanFactory, ThreadPoolTaskExecutor poolTaskExecutor) {
		SimpleApplicationEventMulticaster simpleApplicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
		simpleApplicationEventMulticaster.setTaskExecutor(poolTaskExecutor);
		return simpleApplicationEventMulticaster;
	}

	@Bean
	public ErrorHandler getErrorHandler() {
		ErrorHandler errorHandler = new MyErrorHandler();
		return errorHandler;
	}
}
