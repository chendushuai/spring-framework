package com.chenss.dao;

import org.springframework.stereotype.Component;

@Component
public class AopAspectJDao {
	public void print(String name) {
		System.out.println(String.format("AopAspectJDao print %s",name));
	}
}
