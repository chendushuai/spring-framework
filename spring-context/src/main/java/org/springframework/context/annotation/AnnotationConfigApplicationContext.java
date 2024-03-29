/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.context.annotation;

import java.util.function.Supplier;

import groovy.util.logging.Slf4j;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Spring中出来注解Bean定义的类有两个：
 * {@link AnnotationConfigApplicationContext}和
 * {@link org.springframework.web.context.support.AnnotationConfigWebApplicationContext}。
 * AnnotationConfigWebApplicationContext
 * 是AnnotationConfigApplicationContext的web版本
 * 两者的用法以及对注解的处理方式几乎没有什么差别
 * 通过分析这个类我们知道注册一个bean到spring容器有两种办法
 * 一、直接将注解Bean注册到容器中：（参考）public void register(Class<?>... annotatedClasses)
 * 但是直接把一个注解的bean注册到容器当中也分为两种方法
 * 1、在初始化容器时注册并且解析
 * 2、也可以在容器创建之后手动调用注册方法向容器注册，然后通过手动刷新容器，使得容器对注册的注解Bean进行处理。
 * 思考：为什么@profile要使用这类的第2种方法
 *
 * 二、通过扫描指定的包及其子包下的所有类
 * 扫描其实同上，也是两种方法，初始化的时候扫描，和初始化之后再扫描
 *
 * 独立的应用程序上下文，接受带注释的类作为输入 ——
 * 特别是{@link Configuration @Configuration}-注解类，也可以是普通的
 * {@link org.springframework.stereotype.Component @Component}
 * 类型和使用{@code javax.inject}注解的JSR-330兼容类。
 * 一种是使用{@link #register(Class...)}逐个注册类，
 * 另一种是使用{@link #scan(String...)}进行类路径扫描。
 *
 * <p>同时配置多个{@code @Configuration}类，后面类中定义的@{@link Bean}方法将覆盖前面类中定义的方法。
 * 可以利用这一点，通过额外的{@code @Configuration}类故意覆盖某些bean定义。
 *
 * <p>查看@{@link Configuration}的javadoc获取使用示例。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.0
 * @see #register
 * @see #scan
 * @see AnnotatedBeanDefinitionReader
 * @see ClassPathBeanDefinitionScanner
 * @see org.springframework.context.support.GenericXmlApplicationContext
 */
@Slf4j
public class AnnotationConfigApplicationContext extends GenericApplicationContext implements AnnotationConfigRegistry {

	/**
	 * 在构造方法中实例化的
	 * 用于读取被加了注解的Bean
	 * 这个类顾名思义是一个reader，一个读取器
	 * 读取什么呢？还是顾名思义AnnotatedBeanDefinition意思是读取一个被加了注解的bean
	 * 这个类在构造方法中实例化的
	 */
	private final AnnotatedBeanDefinitionReader reader;

	/**
	 * 同意顾名思义，这是一个扫描器，扫描所有加了注解的bean
	 *  同样是在构造方法中被实例化的
	 */
	private final ClassPathBeanDefinitionScanner scanner;


	/**
	 * M01 初始化一个bean的读取和扫描器
	 * 何谓读取器和扫描器参考上面的属性注释
	 * 默认构造函数，如果直接调用这个默认构造方法，需要在稍后通过调用其register()
	 * 去注册配置类（javaconfig），并调用refresh()方法刷新容器，
	 * 触发容器对注解Bean的载入、解析和注册过程
	 * 这种使用过程我在ioc应用的第二节课讲@profile的时候讲过
	 */
	public AnnotationConfigApplicationContext() {
		/**
		 * 父类的构造方法
		 * C01.04 创建一个读取注解的Bean定义(BeanDefinition)读取器
		 * 什么是bean定义？BeanDefinition
		 */
		System.out.println("C01.04 创建一个读取注解的Bean定义(BeanDefinition)读取器");
		this.reader = new AnnotatedBeanDefinitionReader(this);

		/**
		 * C01.05 可以用来扫描包或者类，继而转换成bd
		 * 但是实际上我们扫描包工作并不是scanner这个对象来完成的
		 * 是spring自己new的一个ClassPathBeanDefinitionScanner
		 * 这里的scanner仅仅是为了程序员能够在外部调用AnnotationConfigApplicationContext对象的scan()方法
		 */
		this.scanner = new ClassPathBeanDefinitionScanner(this);
	}

	/**
	 * Create a new AnnotationConfigApplicationContext with the given DefaultListableBeanFactory.
	 * @param beanFactory the DefaultListableBeanFactory instance to use for this context
	 */
	public AnnotationConfigApplicationContext(DefaultListableBeanFactory beanFactory) {
		super(beanFactory);
		this.reader = new AnnotatedBeanDefinitionReader(this);
		this.scanner = new ClassPathBeanDefinitionScanner(this);
	}

	/**
	 * 这个构造方法需要传入一个被javaconfig注解了的配置类
	 * 然后会把这个被注解了javaconfig的类通过注解读取器读取后继而解析
	 *
	 * <p>创建一个新的AnnotationConfigApplicationContext，从给定的带注释的类派生bean定义，并自动刷新上下文。
	 * @param annotatedClasses 一个或多个注解类，例如：{@link Configuration @Configuration}类
	 */
	public AnnotationConfigApplicationContext(Class<?>... annotatedClasses) {
		this();
		register(annotatedClasses);
		refresh();
	}

	/**
	 * Create a new AnnotationConfigApplicationContext, scanning for bean definitions
	 * in the given packages and automatically refreshing the context.
	 * @param basePackages the packages to check for annotated classes
	 */
	public AnnotationConfigApplicationContext(String... basePackages) {
		this();
		scan(basePackages);
		refresh();
	}


	/**
	 * Propagates the given custom {@code Environment} to the underlying
	 * {@link AnnotatedBeanDefinitionReader} and {@link ClassPathBeanDefinitionScanner}.
	 */
	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		super.setEnvironment(environment);
		this.reader.setEnvironment(environment);
		this.scanner.setEnvironment(environment);
	}

	/**
	 * Provide a custom {@link BeanNameGenerator} for use with {@link AnnotatedBeanDefinitionReader}
	 * and/or {@link ClassPathBeanDefinitionScanner}, if any.
	 * <p>Default is {@link org.springframework.context.annotation.AnnotationBeanNameGenerator}.
	 * <p>Any call to this method must occur prior to calls to {@link #register(Class...)}
	 * and/or {@link #scan(String...)}.
	 * @see AnnotatedBeanDefinitionReader#setBeanNameGenerator
	 * @see ClassPathBeanDefinitionScanner#setBeanNameGenerator
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.reader.setBeanNameGenerator(beanNameGenerator);
		this.scanner.setBeanNameGenerator(beanNameGenerator);
		getBeanFactory().registerSingleton(
				AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR, beanNameGenerator);
	}

	/**
	 * Set the {@link ScopeMetadataResolver} to use for detected bean classes.
	 * <p>The default is an {@link AnnotationScopeMetadataResolver}.
	 * <p>Any call to this method must occur prior to calls to {@link #register(Class...)}
	 * and/or {@link #scan(String...)}.
	 */
	public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
		this.reader.setScopeMetadataResolver(scopeMetadataResolver);
		this.scanner.setScopeMetadataResolver(scopeMetadataResolver);
	}


	//---------------------------------------------------------------------
	// Implementation of AnnotationConfigRegistry
	//---------------------------------------------------------------------

	/**
	 * 注册单个bean给容器
	 * 比如有新加的类可以用这个方法
	 * 但是注册注册之后需要手动调用refresh方法去触发容器解析注解
	 *
	 * 有两个意思
	 * 他可以注册一个配置类
	 * 他还可以单独注册一个bean
	 *
	 * <p>注意，必须调用{@link #refresh()}，以便上下文能够完全处理新类。
	 * @param annotatedClasses 一个或多个注解类，例如：{@link Configuration @Configuration}类
	 * @see #scan(String...)
	 * @see #refresh()
	 */
	public void register(Class<?>... annotatedClasses) {
		Assert.notEmpty(annotatedClasses, "At least one annotated class must be specified");
		// C02.01 使用bean定义读取器加载指定的类，可以是注解配置类，也可以是单独的bean
		this.reader.register(annotatedClasses);
	}

	/**
	 * Perform a scan within the specified base packages.
	 * <p>Note that {@link #refresh()} must be called in order for the context
	 * to fully process the new classes.
	 * @param basePackages the packages to check for annotated classes
	 * @see #register(Class...)
	 * @see #refresh()
	 */
	public void scan(String... basePackages) {
		Assert.notEmpty(basePackages, "At least one base package must be specified");
		this.scanner.scan(basePackages);
	}


	//---------------------------------------------------------------------
	// Adapt superclass registerBean calls to AnnotatedBeanDefinitionReader
	//---------------------------------------------------------------------

	@Override
	public <T> void registerBean(@Nullable String beanName, Class<T> beanClass,
			@Nullable Supplier<T> supplier, BeanDefinitionCustomizer... customizers) {

		this.reader.registerBean(beanClass, beanName, supplier, customizers);
	}

}
