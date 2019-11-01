package com.chenss.config;

import com.chenss.dao.BeanTestA;
import com.chenss.dao.BeanTestB;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.stereotype.Component;

@Configuration
@Component
@ComponentScan("com.chenss")
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
