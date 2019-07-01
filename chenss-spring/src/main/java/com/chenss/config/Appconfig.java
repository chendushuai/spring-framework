package com.chenss.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ComponentScan("com.chenss")
@ImportResource("classpath:spring.xml")
public class Appconfig {
}
