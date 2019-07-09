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
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.Nullable;

/**
 * BeanPostProcessor是Spring框架的提供的一个扩展类点（不止一个）
 * <p>通过实现BeanPostProcessor接口，程序员就可插手bean实例化的过程,从而减轻了beanFactory的负担
 * 值得说明的是这个接口可以设置多个，会形成一个列表，然后依次执行，可以实现
 * {@link PriorityOrdered}接口的{@link PriorityOrdered#getOrder()}来对于多个BeanPostProcessor进行排序。
 * (但是spring默认的怎么办？使用setter方式，进行指定)
 * 比如AOP就是在bean实例后期间将切面逻辑织入bean实例中的
 * AOP也正是通过BeanPostProcessor和IOC容器建立起了联系
 * <p>（由spring提供的默认的PostPorcessor,spring提供了很多默认的PostProcessor,下面我会一一介绍这些实现类的功能）
 * 可以来演示一下 BeanPostProcessor 的使用方式（把动态代理和IOC、aop结合起来使用）
 * 在演示之前先来熟悉一下这个接口，其实这个接口本身特别简单，简单到你发指
 * 但是他的实现类特别复杂，同样复杂到发指！
 * 可以看看spring提供哪些默认的实现（前方高能）
 * <p>查看类的关系图可以知道spring提供了以下的默认实现，因为高能，故而我们只是解释几个常用的
 * <p> 1、ApplicationContextAwareProcessor （acap）
 * <p>   acap后置处理器的作用是：当应用程序定义的Bean实现ApplicationContextAware接口时注入ApplicationContext对象，
 *    也就是，如果类实现了该接口，可以通过
 *    {@link org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)}
 *    方法获取当前的ApplicationContext对象
 * <p>   当然这是他的第一个作业，他还有其他作用，这里不一一列举了，可以参考源码
 *    我们可以针对ApplicationContextAwareProcessor写一个栗子
 * <p> 2、InitDestroyAnnotationBeanPostProcessor
 * <p>    用来处理自定义的初始化方法和销毁方法，
 *    上次说过Spring中提供了3种自定义初始化和销毁方法分别是
 * <p>   一、通过@Bean指定init-method和destroy-method属性
 * <p>   二、Bean实现InitializingBean接口和实现DisposableBean
 * <p>   三、@PostConstruct：@PreDestroy
 * <p>   为什么spring通这三种方法都能完成对bean生命周期的回调呢？
 *    可以通过InitDestroyAnnotationBeanPostProcessor的源码来解释
 * <p> 3、InstantiationAwareBeanPostProcessor
 * <p> 4、CommonAnnotationBeanPostProcessor
 * <p> 5、AutowiredAnnotationBeanPostProcessor
 * <p> 6、RequiredAnnotationBeanPostProcessor
 * <p> 7、BeanValidationPostProcessor
 * <p> 8、AbstractAutoProxyCreator
 *  ......
 *  后面会一一解释
 *
 * <p>工厂钩子，允许自定义修改新的bean实例，例如检查标记接口或用代理包装它们。
 *
 * <p>ApplicationContext可以自动检测bean定义中的BeanPostProcessor bean，并将它们应用于随后创建的任何bean。
 * 普通bean工厂允许编程注册后处理程序，适用于通过该工厂创建的所有bean。
 *
 * <p>通常，通过标记接口或类似方法填充bean的后处理程序将实现{@link #postProcessBeforeInitialization}，
 * 而使用代理包装bean的后处理程序通常将实现{@link #postProcessAfterInitialization}。
 *
 * @author Juergen Hoeller
 * @since 10.10.2003
 * @see InstantiationAwareBeanPostProcessor
 * @see DestructionAwareBeanPostProcessor
 * @see ConfigurableBeanFactory#addBeanPostProcessor
 * @see BeanFactoryPostProcessor
 */
public interface BeanPostProcessor {

	/**
	 * 在任何bean初始化回调(如InitializingBean的{@code afterPropertiesSet}或自定义初始化方法init-method)<i>之前</i>，
	 * 将此BeanPostProcessor应用于给定的新bean实例。bean中已经填充了属性值。返回的bean实例可能是原始bean的包装器。
	 * <p>默认实现按照原样返回给定的{@code bean}。
	 * @param bean 新的bean示例
	 * @param beanName bean的名称
	 * @return 要使用的bean实例，无论是原始的还是包装的实例;如果{@code null}，则不会调用后续的BeanPostProcessor
	 * @throws org.springframework.beans.BeansException 以防出错
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 */
	@Nullable
	default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	/**
	 * 在任何bean初始化回调<i>之后</i>(比如InitializingBean的{@code afterPropertiesSet}或自定义初始化方法init-method)，
	 * 将这个BeanPostProcessor应用到给定的新bean实例。bean中已经填充了属性值。返回的bean实例可能是原始bean的包装器。
	 * <p>对于FactoryBean，这个回调将同时为FactoryBean实例和FactoryBean创建的对象调用(从Spring 2.0开始)。
	 * 后处理器可以通过相应的{@code bean instanceof FactoryBean}检查来决定是应用于FactoryBean还是应用于创建的对象，或者两者都应用。
	 * <p>与所有其他BeanPostProcessor回调不同，这个回调也将在
	 * {@link InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation} 方法触发短路后调用。
	 * This callback will also be invoked after a short-circuiting triggered by a
	 * {@link InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation} method,
	 * in contrast to all other BeanPostProcessor callbacks.
	 * <p>The default implementation returns the given {@code bean} as-is.
	 * @param bean the new bean instance
	 * @param beanName the name of the bean
	 * @return the bean instance to use, either the original or a wrapped one;
	 * if {@code null}, no subsequent BeanPostProcessors will be invoked
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 * @see org.springframework.beans.factory.FactoryBean
	 */
	@Nullable
	default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
