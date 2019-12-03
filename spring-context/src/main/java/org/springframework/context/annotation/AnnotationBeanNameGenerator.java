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

import java.beans.Introspector;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.beans.factory.support.BeanNameGenerator}实现了使用
 * {@link org.springframework.stereotype.Component @Component}注解或使用
 * {@link org.springframework.stereotype.Component @Component}作为其元注解的注解的bean类。
 * 例如，Spring的构造型注解（例如{@link org.springframework.stereotype.Repository @Repository}）
 * 本身是用{@link org.springframework.stereotype.Component @Component}注解的。
 *
 * <p>如果可用的话，也支持Java EE 6的{@link javax.annotation.ManagedBean}和
 * JSR-330的{@link javax.inject.Named}注解。注意Spring组件注解通常重写了这些标准注解。
 *
 * <p>如果注释的值不指示bean名称，则将根据类的短名称（第一个字母为lower-cased）构建适当的名称，也就是默认名称的由来。 例如：
 *
 * <pre class="code">com.xyz.FooServiceImpl -&gt; fooServiceImpl</pre>
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.5
 * @see org.springframework.stereotype.Component#value()
 * @see org.springframework.stereotype.Repository#value()
 * @see org.springframework.stereotype.Service#value()
 * @see org.springframework.stereotype.Controller#value()
 * @see javax.inject.Named#value()
 */
public class AnnotationBeanNameGenerator implements BeanNameGenerator {

	/**
	 * 用于组件扫描的默认{@code AnnotationBeanNameGenerator}实例的简单常量。
	 * @since 5.2
	 */
	public static final AnnotationBeanNameGenerator INSTANCE = new AnnotationBeanNameGenerator();

	private static final String COMPONENT_ANNOTATION_CLASSNAME = "org.springframework.stereotype.Component";

	@Override
	public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
		// C02.01.01_1.01.05.01 判断bean定义是否是注解bean定义
		if (definition instanceof AnnotatedBeanDefinition) {
			// C02.01.01_1.01.05.02 从类的一个注解中生成bean的名称
			String beanName = determineBeanNameFromAnnotation((AnnotatedBeanDefinition) definition);
			// C02.01.01_1.01.05.03 如果在注解中已经明确支出bean的名称，则直接返回指定的名称
			if (StringUtils.hasText(beanName)) {
				// 找到显式bean名称。
				return beanName;
			}
		}
		// C02.01.01_1.01.05.04 生成唯一的缺省bean名称。
		return buildDefaultBeanName(definition, registry);
	}

	/**
	 * M02.01.01_1.01.05.02 从类中的一个注解派生bean名称。
	 * @param annotatedDef 注释织入bean定义
	 * @return bean 的名称，如果找不到，则返回{@code null}
	 */
	@Nullable
	protected String determineBeanNameFromAnnotation(AnnotatedBeanDefinition annotatedDef) {
		AnnotationMetadata amd = annotatedDef.getMetadata();
		Set<String> types = amd.getAnnotationTypes();
		String beanName = null;
		// C02.01.01_1.01.05.02.01 遍历注解类型
		for (String type : types) {
			AnnotationAttributes attributes = AnnotationConfigUtils.attributesFor(amd, type);
			// C02.01.01_1.01.05.02.01_1 判断是否有@Component注解或@Indexed注解，且注解中通过value属性设置了bean的名称
			if (attributes != null && isStereotypeWithNameValue(type, amd.getMetaAnnotationTypes(type), attributes)) {
				// 得到属性值
				Object value = attributes.get("value");
				if (value instanceof String) {
					String strVal = (String) value;
					if (StringUtils.hasLength(strVal)) {
						// 判断是否存在多个注解建议了bean名称，且建议的名称不一致
						if (beanName != null && !strVal.equals(beanName)) {
							throw new IllegalStateException("Stereotype annotations suggest inconsistent " +
									"component names: '" + beanName + "' versus '" + strVal + "'");
						}
						beanName = strVal;
					}
				}
			}
		}
		return beanName;
	}

	/**
	 * 检查给定的注释是否是允许通过注解{@code value()}建议组件名称的原型。
	 * @param annotationType 要检查的注释类的名称
	 * @param metaAnnotationTypes the names of meta-annotations on the given annotation
	 * @param attributes the map of attributes for the given annotation
	 * @return whether the annotation qualifies as a stereotype with component name
	 */
	protected boolean isStereotypeWithNameValue(String annotationType,
			Set<String> metaAnnotationTypes, @Nullable Map<String, Object> attributes) {

		// 是否有指定bean名称的注解
		boolean isStereotype = annotationType.equals(COMPONENT_ANNOTATION_CLASSNAME) ||
				metaAnnotationTypes.contains(COMPONENT_ANNOTATION_CLASSNAME) ||
				annotationType.equals("javax.annotation.ManagedBean") ||
				annotationType.equals("javax.inject.Named");

		return (isStereotype && attributes != null && attributes.containsKey("value"));
	}

	/**
	 * M02.01.01_1.01.05.04 从给定的bean定义派生一个默认bean名称。
	 * <p>默认的实现委托给{@link #buildDefaultBeanName(BeanDefinition)}。
	 * @param definition 要为其构建bean名称的bean定义
	 * @param registry 给定bean定义正在注册的注册表
	 * @return 默认的bean名称(不会是{@code null})
	 */
	protected String buildDefaultBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
		return buildDefaultBeanName(definition);
	}

	/**
	 * M02.01.01_1.01.05.04.01 从给定的bean定义派生一个默认bean名称。
	 * <p>默认实现只是简单地构建一个简短类名的非斜体版本：例如。“mypackage.MyJdbcDao”- >“myJdbcDao”。
	 * <p>注意，内部类将因此具有表单“outerClassName.InnerClassName”的名称。
	 * 由于名称中的句点，如果按名称进行自动装配，则可能会出现问题。
	 * @param definition 要为其构建bean名称的bean定义
	 * @return 默认bean名称(不会是 {@code null})
	 */
	protected String buildDefaultBeanName(BeanDefinition definition) {
		String beanClassName = definition.getBeanClassName();
		Assert.state(beanClassName != null, "No bean class name set");
		String shortClassName = ClassUtils.getShortName(beanClassName);
		return Introspector.decapitalize(shortClassName);
	}

}
