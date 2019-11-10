package com.chenss.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Aspect("perthis(this(com.chenss.dao.AopPrototypeAspectJDao))")
@Scope("prototype")
public class AopPrototypeAspectJ {
	@Pointcut("execution(* com.chenss.dao.AopPrototypeAspectJDao.*(..))")
	public void pointCut() {

	}
	@Around("pointCut()")
	public void around(ProceedingJoinPoint pjp) {
		System.out.println("AopPrototypeAspectJ aop AopPrototypeAspectJDao. hashcode:" + this.hashCode());
		try {
			pjp.proceed(pjp.getArgs());
		} catch (Throwable throwable) {
			throwable.printStackTrace();
		}
		System.out.println("AopPrototypeAspectJ aop AopPrototypeAspectJDao. end");
	}
}
