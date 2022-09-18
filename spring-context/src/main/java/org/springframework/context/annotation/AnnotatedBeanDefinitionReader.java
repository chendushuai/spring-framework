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

import java.lang.annotation.Annotation;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 方便的适配器，可编程注册带注解的bean类。
 * 这是{@link ClassPathBeanDefinitionScanner}的另一种选择，它应用了相同的注解解析，但是只针对显式注册的类。
 *
 * AnnotatedBeanDefinitionReader仅用来生成显式注册的类的bean定义
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 3.0
 * @see AnnotationConfigApplicationContext#register
 */
public class AnnotatedBeanDefinitionReader {

	private final BeanDefinitionRegistry registry;

	private BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;

	/**
	 * 注解范围解析器
	 */
	private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();

	/**
	 * 用于计算{@link Conditional}注解
	 */
	private ConditionEvaluator conditionEvaluator;


	/**
	 * M01.04 使用构造方法创建一个注解bean定义读取器
	 * 这里的BeanDefinitionRegistry registry是通过在AnnotationConfigApplicationContext
	 * 的构造方法中传进来的this
	 * 由此说明AnnotationConfigApplicationContext是一个BeanDefinitionRegistry类型的类
	 * 何以证明我们可以看到AnnotationConfigApplicationContext的类关系：
	 * GenericApplicationContext extends AbstractApplicationContext implements BeanDefinitionRegistry
	 * 看到他实现了BeanDefinitionRegistry证明上面的说法，那么BeanDefinitionRegistry的作用是什么呢？
	 * BeanDefinitionRegistry 顾名思义就是BeanDefinition的注册器
	 * 那么何为BeanDefinition呢？参考BeanDefinition的源码的注释
	 */
	public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry) {
		/**
		 * C01.04.01 生成一个Reader，添加后置处理器，解析bean定义，Config文件等
		 */
		this(registry, getOrCreateEnvironment(registry));
	}

	/**
	 * M01.04.01 为给定注册表创建一个新的{@code AnnotatedBeanDefinitionReader}，并使用给定的{@link Environment}。
	 * @param registry 以{@code BeanDefinitionRegistry}的形式将bean定义加载到{@code BeanFactory}中
	 * @param environment 在评估bean定义概要文件时要使用的{@code Environment}。
	 * @since 3.1
	 */
	public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry, Environment environment) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		Assert.notNull(environment, "Environment must not be null");
		this.registry = registry;
		// 创建用于计算{@link Conditional}注解类
		this.conditionEvaluator = new ConditionEvaluator(registry, environment, null);
		// C01.04.01_1.01 在给定注册表中注册所有相关的注解配置处理器。
		AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
	}


	/**
	 * Return the BeanDefinitionRegistry that this scanner operates on.
	 */
	public final BeanDefinitionRegistry getRegistry() {
		return this.registry;
	}

	/**
	 * Set the Environment to use when evaluating whether
	 * {@link Conditional @Conditional}-annotated component classes should be registered.
	 * <p>The default is a {@link StandardEnvironment}.
	 * @see #registerBean(Class, String, Class...)
	 */
	public void setEnvironment(Environment environment) {
		this.conditionEvaluator = new ConditionEvaluator(this.registry, environment, null);
	}

	/**
	 * Set the BeanNameGenerator to use for detected bean classes.
	 * <p>The default is a {@link AnnotationBeanNameGenerator}.
	 */
	public void setBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator =
				(beanNameGenerator != null ? beanNameGenerator : AnnotationBeanNameGenerator.INSTANCE);
	}

	/**
	 * Set the ScopeMetadataResolver to use for detected bean classes.
	 * <p>The default is an {@link AnnotationScopeMetadataResolver}.
	 */
	public void setScopeMetadataResolver(@Nullable ScopeMetadataResolver scopeMetadataResolver) {
		this.scopeMetadataResolver =
				(scopeMetadataResolver != null ? scopeMetadataResolver : new AnnotationScopeMetadataResolver());
	}


	/**
	 * 注册一个或多个要处理的注解类
	 * <p>对{@code register}的调用是幂等的；多次添加同一个带注释的类不会产生额外的效果。
	 * @param annotatedClasses 一个或多个注解类，例如：{@link Configuration @Configuration}类
	 */
	public void register(Class<?>... annotatedClasses) {
		// C02.01.01 遍历添加的注解类，执行注册
		for (Class<?> annotatedClass : annotatedClasses) {
			// C02.01.01_1 执行注册
			registerBean(annotatedClass);
		}
	}

	/**
	 * 注册来自给定bean类的bean，从类声明的注解派生其元数据。
	 * @param annotatedClass bean的Class
	 */
	public void registerBean(Class<?> annotatedClass) {
		// C02.01.01_1.01 执行注册bean
		doRegisterBean(annotatedClass, null, null, null, null);
	}

	/**
	 * 注册来自给定bean类的bean，从类声明的注解派生其元数据。
	 * @param annotatedClass bean的Class
	 * @param name bean的显式名称(或用于生成默认bean名称的{@code null})
	 * @since 5.2
	 */
	public void registerBean(Class<?> annotatedClass, @Nullable String name) {
		doRegisterBean(annotatedClass, name, null, null, null);
	}

	/**
	 * 注册来自给定bean类的bean，从类声明的注解派生其元数据。
	 * @param annotatedClass bean的Class
	 * @param qualifiers 除了bean类级别的限定符之外，还需要考虑特定的限定符注解
	 */
	@SuppressWarnings("unchecked")
	public void registerBean(Class<?> annotatedClass, Class<? extends Annotation>... qualifiers) {
		doRegisterBean(annotatedClass, null, qualifiers, null, null);
	}

	/**
	 * 注册来自给定bean类的bean，从类声明的注解派生其元数据。
	 * @param annotatedClass bean的Class
	 * @param name bean的显式名称(或用于生成默认bean名称的{@code null})
	 * @param qualifiers 除了bean类级别的限定符之外，还需要考虑特定的限定符注解
	 */
	@SuppressWarnings("unchecked")
	public void registerBean(Class<?> annotatedClass, @Nullable String name,
			Class<? extends Annotation>... qualifiers) {

		doRegisterBean(annotatedClass, name, qualifiers, null, null);
	}

	/**
	 * Register a bean from the given bean class, deriving its metadata from
	 * class-declared annotations, using the given supplier for obtaining a new
	 * instance (possibly declared as a lambda expression or method reference).
	 * @param annotatedClass the class of the bean
	 * @param supplier a callback for creating an instance of the bean
	 * (may be {@code null})
	 * @since 5.0
	 */
	public <T> void registerBean(Class<T> annotatedClass, @Nullable Supplier<T> supplier) {
		doRegisterBean(annotatedClass, null, null, supplier, null);
	}

	/**
	 * Register a bean from the given bean class, deriving its metadata from
	 * class-declared annotations, using the given supplier for obtaining a new
	 * instance (possibly declared as a lambda expression or method reference).
	 * @param annotatedClass the class of the bean
	 * @param name an explicit name for the bean
	 * (or {@code null} for generating a default bean name)
	 * @param supplier a callback for creating an instance of the bean
	 * (may be {@code null})
	 * @since 5.0
	 */
	public <T> void registerBean(Class<T> annotatedClass, @Nullable String name, @Nullable Supplier<T> supplier) {
		doRegisterBean(annotatedClass, name, null, supplier, null);
	}

	/**
	 * Register a bean from the given bean class, deriving its metadata from
	 * class-declared annotations.
	 * @param annotatedClass the class of the bean
	 * @param name an explicit name for the bean
	 * (or {@code null} for generating a default bean name)
	 * @param supplier a callback for creating an instance of the bean
	 * (may be {@code null})
	 * @param customizers one or more callbacks for customizing the factory's
	 * {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
	 * @since 5.2
	 */
	public <T> void registerBean(Class<T> annotatedClass, @Nullable String name, @Nullable Supplier<T> supplier,
			BeanDefinitionCustomizer... customizers) {

		doRegisterBean(annotatedClass, name, null, supplier, customizers);
	}

	/**
	 * 注册来自给定bean类的bean，从类声明的注解派生其元数据。
	 * @param annotatedClass bean类
	 * @param name bean的显式名称
	 * @param supplier 用于创建bean实例的回调函数(可能是{@code null})
	 * @param qualifiers 除了bean类级别上的限定符之外，还需要考虑特定的限定符注释(如果有的话)
	 * @param customizers 一个或多个回调函数，用于定制工厂的{@link BeanDefinition}，例如设置一个lazy-init或primary标志
	 * @since 5.0
	 */
	private <T> void doRegisterBean(Class<T> annotatedClass, @Nullable String name,
			@Nullable Class<? extends Annotation>[] qualifiers, @Nullable Supplier<T> supplier,
			@Nullable BeanDefinitionCustomizer[] customizers) {

		// C02.01.01_1.01.01 生成注解bean定义AnnotatedGenericBeanDefinition，这时候Appconfig的bd还没有添加到BeanFactory中
		// 这里很重要，配合后面对于配置类的判断，这里在注册配置类时，直接将配置类创建为AnnotatedGenericBeanDefinition
		AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(annotatedClass);
		if (this.conditionEvaluator.shouldSkip(abd.getMetadata())) {
			return;
		}

		// C02.01.01_1.01.03 指定创建bean实例的回调函数
		abd.setInstanceSupplier(supplier);
		// C02.01.01_1.01.04 解析bean定义的注解范围
		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(abd);
		abd.setScope(scopeMetadata.getScopeName());
		// C02.01.01_1.01.05 生成bean名称
		String beanName = (name != null ? name : this.beanNameGenerator.generateBeanName(abd, this.registry));
		// C02.01.01_1.01.06 处理基本定义注解
		AnnotationConfigUtils.processCommonDefinitionAnnotations(abd);
		// C02.01.01_1.01.07 如果给定了需要特殊处理的限定符，还需要判断处理
		if (qualifiers != null) {
			for (Class<? extends Annotation> qualifier : qualifiers) {
				if (Primary.class == qualifier) {
					abd.setPrimary(true);
				}
				else if (Lazy.class == qualifier) {
					abd.setLazyInit(true);
				}
				else {
					abd.addQualifier(new AutowireCandidateQualifier(qualifier));
				}
			}
		}
		// C02.01.01_1.01.08 如果指定了bean定义的自定义回调函数，则调用该回调函数
		if (customizers != null) {
			for (BeanDefinitionCustomizer customizer : customizers) {
				customizer.customize(abd);
			}
		}
		// C02.01.01_1.01.09 创建bean定义的BeanDefinitionHolder
		BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(abd, beanName);
		definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
		// C02.01.01_1.01.10 进行Bean定义的注册，将AppConfig的配置添加到beanFactory的Bd中
		BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, this.registry);
	}


	/**
	 * 如果可能，从给定的注册表获取环境，否则返回一个新的StandardEnvironment。
	 */
	private static Environment getOrCreateEnvironment(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		if (registry instanceof EnvironmentCapable) {
			return ((EnvironmentCapable) registry).getEnvironment();
		}
		return new StandardEnvironment();
	}

}
