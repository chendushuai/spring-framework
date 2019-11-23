/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 定义搜索与元数据相关的方法的算法，这些方法详尽地包括接口和父类，
 * 同时还处理参数化方法以及使用接口和基于类的代理遇到的常见场景。
 *
 * <p>通常(但不一定)用于查找带注解的处理程序方法。
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 4.2.3
 */
public final class MethodIntrospector {

	private MethodIntrospector() {
	}


	/**
	 * Select methods on the given target type based on the lookup of associated metadata.
	 * <p>Callers define methods of interest through the {@link MetadataLookup} parameter,
	 * allowing to collect the associated metadata into the result map.
	 * @param targetType the target type to search methods on
	 * @param metadataLookup a {@link MetadataLookup} callback to inspect methods of interest,
	 * returning non-null metadata to be associated with a given method if there is a match,
	 * or {@code null} for no match
	 * @return the selected methods associated with their metadata (in the order of retrieval),
	 * or an empty map in case of no match
	 */
	public static <T> Map<Method, T> selectMethods(Class<?> targetType, final MetadataLookup<T> metadataLookup) {
		final Map<Method, T> methodMap = new LinkedHashMap<>();
		Set<Class<?>> handlerTypes = new LinkedHashSet<>();
		Class<?> specificHandlerType = null;

		if (!Proxy.isProxyClass(targetType)) {
			specificHandlerType = ClassUtils.getUserClass(targetType);
			handlerTypes.add(specificHandlerType);
		}
		handlerTypes.addAll(ClassUtils.getAllInterfacesForClassAsSet(targetType));

		for (Class<?> currentHandlerType : handlerTypes) {
			final Class<?> targetClass = (specificHandlerType != null ? specificHandlerType : currentHandlerType);

			ReflectionUtils.doWithMethods(currentHandlerType, method -> {
				Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
				T result = metadataLookup.inspect(specificMethod);
				if (result != null) {
					Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
					if (bridgedMethod == specificMethod || metadataLookup.inspect(bridgedMethod) == null) {
						methodMap.put(specificMethod, result);
					}
				}
			}, ReflectionUtils.USER_DECLARED_METHODS);
		}

		return methodMap;
	}

	/**
	 * Select methods on the given target type based on a filter.
	 * <p>Callers define methods of interest through the {@code MethodFilter} parameter.
	 * @param targetType the target type to search methods on
	 * @param methodFilter a {@code MethodFilter} to help
	 * recognize handler methods of interest
	 * @return the selected methods, or an empty set in case of no match
	 */
	public static Set<Method> selectMethods(Class<?> targetType, final ReflectionUtils.MethodFilter methodFilter) {
		return selectMethods(targetType,
				(MetadataLookup<Boolean>) method -> (methodFilter.matches(method) ? Boolean.TRUE : null)).keySet();
	}

	/**
	 * 在目标类型上选择一个可调用的方法:
	 * 如果实际暴露在目标类型上，则选择给定的方法本身，或者在目标类型的某个接口上选择相应的方法，或者在目标类型本身上选择相应的方法。
	 * <p>用户声明的接口上的匹配将是首选的，因为它们可能包含与目标类上的方法相对应的相关元数据。
	 * @param method 要检查的方法
	 * @param targetType 要搜索方法的目标类型(通常是基于接口的JDK代理)
	 * @return 目标类型上相应的可调用方法
	 * @throws IllegalStateException 如果给定的方法不能在给定的目标类型上调用(通常是由于代理不匹配)
	 */
	public static Method selectInvocableMethod(Method method, Class<?> targetType) {
		if (method.getDeclaringClass().isAssignableFrom(targetType)) {
			return method;
		}
		try {
			String methodName = method.getName();
			Class<?>[] parameterTypes = method.getParameterTypes();
			for (Class<?> ifc : targetType.getInterfaces()) {
				try {
					return ifc.getMethod(methodName, parameterTypes);
				}
				catch (NoSuchMethodException ex) {
					// Alright, not on this interface then...
				}
			}
			// A final desperate attempt on the proxy class itself...
			return targetType.getMethod(methodName, parameterTypes);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException(String.format(
					"Need to invoke method '%s' declared on target class '%s', " +
					"but not found in any interface(s) of the exposed proxy type. " +
					"Either pull the method up to an interface or switch to CGLIB " +
					"proxies by enforcing proxy-target-class mode in your configuration.",
					method.getName(), method.getDeclaringClass().getSimpleName()));
		}
	}


	/**
	 * A callback interface for metadata lookup on a given method.
	 * @param <T> the type of metadata returned
	 */
	@FunctionalInterface
	public interface MetadataLookup<T> {

		/**
		 * Perform a lookup on the given method and return associated metadata, if any.
		 * @param method the method to inspect
		 * @return non-null metadata to be associated with a method if there is a match,
		 * or {@code null} for no match
		 */
		@Nullable
		T inspect(Method method);
	}

}
