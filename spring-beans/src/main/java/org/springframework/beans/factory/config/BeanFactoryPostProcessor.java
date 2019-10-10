/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.beans.BeansException;

/**
 * 允许自定义修改应用程序上下文的bean定义，调整上下文的底层bean工厂的bean属性值。
 *
 * <p>应用程序上下文可以自动检测bean定义中的BeanFactoryPostProcessor bean，并在创建任何其他bean之前应用它们。
 *
 * <p>适用于针对覆盖在应用程序上下文中配置的bean属性的系统管理员的自定义配置文件。
 *
 * <p>有关解决此类配置需求的开箱即用解决方案，请参见PropertyResourceConfigurer及其具体实现。
 *
 * <p>BeanFactoryPostProcessor可以与bean定义交互并修改bean定义，但不能与bean实例交互。
 * 这样做可能会导致过早的bean实例化，破坏容器并导致意想不到的副作用。
 * 如果需要bean实例交互，则考虑实现{@link BeanPostProcessor}。
 *
 * <p>spring的扩展点之一
 * <p>实现该接口，可以在spring的bean创建之前修改bean的定义属性。
 * <p>spring允许BeanFactoryPostProcessor在容器实例化任何其它bean之前读取配置元数据，
 * <p>并可以根据需要进行修改，例如可以把bean的scope从singleton改为prototype，也可以把property的值给修改掉。
 * <p>可以同时配置多个BeanFactoryPostProcessor，并通过设置'order'属性来控制各个BeanFactoryPostProcessor的执行次序。
 * <p>BeanFactoryPostProcessor是在spring容器加载了bean的定义文件之后，在bean实例化之前执行的
 * <p>可以写一个栗子来测试一下这个功能
 *
 * @author Juergen Hoeller
 * @since 06.07.2003
 * @see BeanPostProcessor
 * @see PropertyResourceConfigurer
 */
@FunctionalInterface
public interface BeanFactoryPostProcessor {

	/**
	 * 在应用程序上下文的标准初始化之后修改其内部bean工厂。
	 * 所有bean定义都已加载，但还没有实例化bean。
	 * 这允许覆盖或添加属性，甚至是对急于初始化的bean。
	 * @param beanFactory 应用程序上下文使用的bean工厂
	 * @throws org.springframework.beans.BeansException 错误的情况
	 */
	void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;

}
