/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.beans.factory.config;

import org.springframework.lang.Nullable;

/**
 * Interface that defines a registry for shared bean instances.
 * Can be implemented by {@link org.springframework.beans.factory.BeanFactory}
 * implementations in order to expose their singleton management facility
 * in a uniform manner.
 *
 * <p>The {@link ConfigurableBeanFactory} interface extends this interface.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see ConfigurableBeanFactory
 * @see org.springframework.beans.factory.support.DefaultSingletonBeanRegistry
 * @see org.springframework.beans.factory.support.AbstractBeanFactory
 */
public interface SingletonBeanRegistry {

	/**
	 * 在bean注册表中，在给定的bean名称下，将给定的现有对象注册为单例对象。
	 * <p>给定的实例应该被完全初始化;
	 * 注册表不会执行任何初始化回调(特别是，它不会调用InitializingBean的{@code afterPropertiesSet}方法)。
	 * 给定的实例也不会收到任何销毁回调(比如DisposableBean的{@code destroy}方法)。
	 * <p>当运行在一个完整的BeanFactory时:
	 * <b>如果您的bean应该接收初始化和/或销毁回调，那么注册一个bean定义而不是一个现有实例。</b>
	 * <p>通常在注册表配置期间调用，但也可用于单例的运行时注册。
	 * 因此，注册表实现应该同步单例访问;
	 * 如果它支持BeanFactory对单例对象的延迟初始化，那么无论如何它都必须这样做。
	 * @param beanName bean的名称
	 * @param singletonObject 现有的单例对象
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 * @see org.springframework.beans.factory.DisposableBean#destroy
	 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry#registerBeanDefinition
	 */
	void registerSingleton(String beanName, Object singletonObject);

	/**
	 * 返回在给定名称下注册的(原始)单例对象。
	 * <p>只检查已经实例化的单例对象;未为尚未实例化的单例bean定义返回对象。
	 * <p>这个方法的主要目的是访问手动注册的单例对象(参见{@link #registerSingleton})。
	 * 还可以用于访问已经以原始方式创建的bean定义定义的单例。
	 * <p><b>注意:</b> 此查找方法不知道FactoryBean前缀或别名。
	 * 在获得单例实例之前，需要先解析规范bean名。
	 * @param beanName 要查找的bean的名称
	 * @return 已注册的单例对象，如果找不到就返回{@code null}
	 * @see ConfigurableListableBeanFactory#getBeanDefinition
	 */
	@Nullable
	Object getSingleton(String beanName);

	/**
	 * 检查此注册表是否包含具有给定名称的单例实例。
	 * <p>只检查已经实例化的单例对象;对于尚未实例化的单例bean定义，不返回{@code true}。
	 * <p>这个方法的主要目的是检查手动注册的单例对象(参见{@link #registerSingleton})。
	 * 还可以用来检查由bean定义定义的单例是否已经创建。
	 * <p>要检查bean工厂是否包含具有给定名称的bean定义，请使用ListableBeanFactory的{@code containsBeanDefinition}。
	 * 调用{@code containsBeanDefinition}和{@code containsSingleton}
	 * 可以回答特定的bean工厂是否包含具有给定名称的本地bean实例。
	 * <p>使用BeanFactory的{@code containsBean}用于常规检查工厂是否知道具有给定名称的bean
	 * (无论是手动注册的单例实例还是由bean定义创建的)，也检查祖先工厂。
	 * <p><b>注意:</b> 此查找方法不知道FactoryBean前缀或别名。
	 * 在检查单例状态之前，需要先解析规范bean名。
	 * @param beanName 要查找的bean的名称
	 * @return 如果此bean工厂包含具有给定名称的单例实例
	 * @see #registerSingleton
	 * @see org.springframework.beans.factory.ListableBeanFactory#containsBeanDefinition
	 * @see org.springframework.beans.factory.BeanFactory#containsBean
	 */
	boolean containsSingleton(String beanName);

	/**
	 * Return the names of singleton beans registered in this registry.
	 * <p>Only checks already instantiated singletons; does not return names
	 * for singleton bean definitions which have not been instantiated yet.
	 * <p>The main purpose of this method is to check manually registered singletons
	 * (see {@link #registerSingleton}). Can also be used to check which singletons
	 * defined by a bean definition have already been created.
	 * @return the list of names as a String array (never {@code null})
	 * @see #registerSingleton
	 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry#getBeanDefinitionNames
	 * @see org.springframework.beans.factory.ListableBeanFactory#getBeanDefinitionNames
	 */
	String[] getSingletonNames();

	/**
	 * Return the number of singleton beans registered in this registry.
	 * <p>Only checks already instantiated singletons; does not count
	 * singleton bean definitions which have not been instantiated yet.
	 * <p>The main purpose of this method is to check manually registered singletons
	 * (see {@link #registerSingleton}). Can also be used to count the number of
	 * singletons defined by a bean definition that have already been created.
	 * @return the number of singleton beans
	 * @see #registerSingleton
	 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry#getBeanDefinitionCount
	 * @see org.springframework.beans.factory.ListableBeanFactory#getBeanDefinitionCount
	 */
	int getSingletonCount();

	/**
	 * Return the singleton mutex used by this registry (for external collaborators).
	 * @return the mutex object (never {@code null})
	 * @since 4.2
	 */
	Object getSingletonMutex();

}
