package com.chenss.config;

import com.chenss.dao.BeanTestA;
import com.chenss.dao.BeanTestB;
import com.chenss.event.MyErrorHandler;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;

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
	public ThreadPoolTaskExecutor getThreadPoolTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5);
		executor.setMaxPoolSize(500);
		return executor;
	}

	@Bean
	public ErrorHandler getErrorHandler() {
		ErrorHandler errorHandler = new MyErrorHandler();
		return errorHandler;
	}
}
