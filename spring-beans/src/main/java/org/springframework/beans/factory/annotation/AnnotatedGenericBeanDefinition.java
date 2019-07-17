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

package org.springframework.beans.factory.annotation;

import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.beans.factory.support.GenericBeanDefinition}类的扩展，
 * 添加了对通过{@link AnnotatedBeanDefinition}接口公开的注释元数据的支持。
 *
 * <p>这个GenericBeanDefinition变体主要用于测试希望在AnnotatedBeanDefinition上操作的代码，
 * 例如Spring组件扫描支持中的策略实现(其中默认定义类是
 * {@link org.springframework.context.annotation.ScannedGenericBeanDefinition}，
 * 它也实现了AnnotatedBeanDefinition接口)。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 2.5
 * @see AnnotatedBeanDefinition#getMetadata()
 * @see org.springframework.core.type.StandardAnnotationMetadata
 */
@SuppressWarnings("serial")
public class AnnotatedGenericBeanDefinition extends GenericBeanDefinition implements AnnotatedBeanDefinition {

	/**
	 * 注解元数据
	 */
	private final AnnotationMetadata metadata;

	/**
	 * 工厂方法元数据
	 */
	@Nullable
	private MethodMetadata factoryMethodMetadata;


	/**
	 * 为给定的bean类创建一个新的AnnotatedGenericBeanDefinition。
	 * @param beanClass 已加载的bean类
	 */
	public AnnotatedGenericBeanDefinition(Class<?> beanClass) {
		setBeanClass(beanClass);
		this.metadata = AnnotationMetadata.introspect(beanClass);
	}

	/**
	 * Create a new AnnotatedGenericBeanDefinition for the given annotation metadata,
	 * allowing for ASM-based processing and avoidance of early loading of the bean class.
	 * Note that this constructor is functionally equivalent to
	 * {@link org.springframework.context.annotation.ScannedGenericBeanDefinition
	 * ScannedGenericBeanDefinition}, however the semantics of the latter indicate that a
	 * bean was discovered specifically via component-scanning as opposed to other means.
	 * @param metadata the annotation metadata for the bean class in question
	 * @since 3.1.1
	 */
	public AnnotatedGenericBeanDefinition(AnnotationMetadata metadata) {
		Assert.notNull(metadata, "AnnotationMetadata must not be null");
		if (metadata instanceof StandardAnnotationMetadata) {
			setBeanClass(((StandardAnnotationMetadata) metadata).getIntrospectedClass());
		}
		else {
			setBeanClassName(metadata.getClassName());
		}
		this.metadata = metadata;
	}

	/**
	 * Create a new AnnotatedGenericBeanDefinition for the given annotation metadata,
	 * based on an annotated class and a factory method on that class.
	 * @param metadata the annotation metadata for the bean class in question
	 * @param factoryMethodMetadata metadata for the selected factory method
	 * @since 4.1.1
	 */
	public AnnotatedGenericBeanDefinition(AnnotationMetadata metadata, MethodMetadata factoryMethodMetadata) {
		this(metadata);
		Assert.notNull(factoryMethodMetadata, "MethodMetadata must not be null");
		setFactoryMethodName(factoryMethodMetadata.getMethodName());
		this.factoryMethodMetadata = factoryMethodMetadata;
	}


	@Override
	public final AnnotationMetadata getMetadata() {
		return this.metadata;
	}

	@Override
	@Nullable
	public final MethodMetadata getFactoryMethodMetadata() {
		return this.factoryMethodMetadata;
	}

}
