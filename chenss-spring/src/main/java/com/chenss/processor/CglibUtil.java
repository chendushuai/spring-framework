package com.chenss.processor;

import com.chenss.dao.UserDao;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

public class CglibUtil {
	public static Object getProxy() {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(UserDao.class);
		enhancer.setUseFactory(false);
		enhancer.setCallback(new MethodInterceptor() {
			@Override
			public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
				System.out.println("cglib aop------------------------------");
				Object invokeResult = methodProxy.invoke(method, objects);
				return invokeResult;
			}
		});
		Object result = enhancer.create();
		return result;
	}
}
