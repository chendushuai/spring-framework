package com.chenss.config;

import com.chenss.dao.BeanTestA;
import com.chenss.dao.BeanTestB;
import com.chenss.processor.CustomAopBeanPostProcessor;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Component;

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
}
