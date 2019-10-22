package com.chenss.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.stereotype.Component;

//@Configuration
@Component
@ComponentScan("com.chenss")
//@ImportResource("classpath:spring.xml")
public class Appconfig {
}
