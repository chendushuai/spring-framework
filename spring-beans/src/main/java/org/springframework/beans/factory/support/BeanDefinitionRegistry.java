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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.AliasRegistry;

/**
 * 用于包含bean定义的注册中心的接口，例如RootBeanDefinition和ChildBeanDefinition实例。
 * 通常由内部使用AbstractBeanDefinition层次结构的bean工厂实现。
 *
 * <p>这是Spring bean工厂包中唯一封装bean定义<i>注册</i>的接口。
 * 标准的BeanFactory接口只覆盖对<i>完全配置的工厂实例</i>的访问。
 *
 * <p>Spring的bean定义读取器希望处理这个接口的实现。
 * Spring核心中已知的实现者是DefaultListableBeanFactory和GenericApplicationContext。
 *
 * @author Juergen Hoeller
 * @since 26.11.2003
 * @see org.springframework.beans.factory.config.BeanDefinition
 * @see AbstractBeanDefinition
 * @see RootBeanDefinition
 * @see ChildBeanDefinition
 * @see DefaultListableBeanFactory
 * @see org.springframework.context.support.GenericApplicationContext
 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
 * @see PropertiesBeanDefinitionReader
 */
public interface BeanDefinitionRegistry extends AliasRegistry {

	/**
	 * 用这个注册表注册一个新的bean定义。必须支持RootBeanDefinition和ChildBeanDefinition。
	 * @param beanName 要注册的bean实例的名称
	 * @param beanDefinition 要注册的bean实例的定义
	 * @throws BeanDefinitionStoreException 如果BeanDefinition无效
	 * @throws BeanDefinitionOverrideException 如果已经有指定bean名称的bean定义，并且不允许覆盖它
	 * @see GenericBeanDefinition
	 * @see RootBeanDefinition
	 * @see ChildBeanDefinition
	 */
	void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException;

	/**
	 * 移除给定名称的BeanDefinition
	 * @param beanName 要注册的bean实例的名称
	 * @throws NoSuchBeanDefinitionException 如果没有匹配的bean定义
	 */
	void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * 返回给定bean名称的bean定义。
	 * @param beanName 想要查找定义的bean名称
	 * @return 给定名称bean的BeanDefinition(不会是{@code null})
	 * @throws NoSuchBeanDefinitionException 如果找不到匹配名称的bean定义
	 */
	BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * 检查此注册表是否包含具有给定名称的bean定义。
	 * @param beanName 用于查找的bean名称
	 * @return 如果注解表中包含给定名称的bean，返回{@code true}，否则返回{@code false}
	 */
	boolean containsBeanDefinition(String beanName);

	/**
	 * 返回此注册表中定义的所有bean的名称。
	 * @return 此注册表中定义的所有bean的名称，如果没有定义，则为空数组
	 */
	String[] getBeanDefinitionNames();

	/**
	 * 返回注册表中定义的bean的数量。
	 * @return 注册表中定义的bean的数量。
	 */
	int getBeanDefinitionCount();

	/**
	 * 确定给定的bean名称是否已经在此注册表中使用，即是否有一个本地bean或别名在此名称下注册。
	 * @param beanName 想要检查的名称
	 * @return 给定名称的bean是否已经被使用
	 */
	boolean isBeanNameInUse(String beanName);

}
