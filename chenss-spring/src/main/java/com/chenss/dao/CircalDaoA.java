package com.chenss.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CircalDaoA {
	@Autowired
	private CircalDaoB circalDaoB;
	public CircalDaoA() {
		System.out.println("CircalDaoA 中 打印的 ");
	}
}
