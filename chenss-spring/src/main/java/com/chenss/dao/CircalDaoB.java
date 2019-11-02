package com.chenss.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CircalDaoB {
	@Autowired
	private CircalDaoA circalDaoA;
	public CircalDaoB() {
		System.out.println("CircalDaoB 中 打印的 ");
	}
}
