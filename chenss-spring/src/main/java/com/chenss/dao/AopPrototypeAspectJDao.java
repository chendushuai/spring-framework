package com.chenss.dao;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class AopPrototypeAspectJDao {
	public void print(String name) {
		System.out.println(String.format("AopPrototypeAspectJDao print %s",name));
	}
}
