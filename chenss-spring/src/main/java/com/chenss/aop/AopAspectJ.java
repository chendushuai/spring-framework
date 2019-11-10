package com.chenss.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AopAspectJ {
	@Pointcut("execution(* com.chenss.dao.AopAspectJDao.*(..))")
	public void pointCut() {

	}

	@Around("pointCut()")
	public void aroud(ProceedingJoinPoint pjp) {
		System.out.println("AopAspectJ aop AopAspectJDao. hashcode:" + this.hashCode());
		Object[] args = pjp.getArgs();
		try {
			pjp.proceed(args);
		} catch (Throwable throwable) {
			throwable.printStackTrace();
		}
		System.out.println("AopAspectJ aop AopAspectJDao end.");
	}
}
