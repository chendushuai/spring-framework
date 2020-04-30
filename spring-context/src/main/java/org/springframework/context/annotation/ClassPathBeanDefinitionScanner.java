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

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionDefaults;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;

/**
 * 一个bean定义扫描器，它检测类路径上的bean候选项，并向给定的注册表({@code BeanFactory}或
 * {@code ApplicationContext})注册相应的bean定义。
 *
 * <p>通过可配置的类型过滤器检测候选类。
 * 默认过滤器包括用Spring的{@link org.springframework.stereotype.Component @Component}、
 * {@link org.springframework.stereotype.Repository @Repository}、
 * {@link org.springframework.stereotype.Service @Service}或者
 * {@link org.springframework.stereotype.Controller @Controller}类型注解的类
 *
 * <p>如果可用的话，也支持Java EE 6的 {@link javax.annotation.ManagedBean}和
 * JSR-330的{@link javax.inject.Named}注解。
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 2.5
 * @see AnnotationConfigApplicationContext#scan
 * @see org.springframework.stereotype.Component
 * @see org.springframework.stereotype.Repository
 * @see org.springframework.stereotype.Service
 * @see org.springframework.stereotype.Controller
 */
public class ClassPathBeanDefinitionScanner extends ClassPathScanningCandidateComponentProvider {

	// 指定bean定义注册器
	private final BeanDefinitionRegistry registry;

	/**
	 * 默认bean定义
	 */
	private BeanDefinitionDefaults beanDefinitionDefaults = new BeanDefinitionDefaults();

	@Nullable
	private String[] autowireCandidatePatterns;

	private BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;

	private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();

	private boolean includeAnnotationConfig = true;


	/**
	 * 为给定的bean工厂创建一个新的{@code ClassPathBeanDefinitionScanner}。
	 * @param registry 以{@code BeanDefinitionRegistry}的形式将bean定义加载到{@code BeanFactory}中
	 */
	public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry) {
		this(registry, true);
	}

	/**
	 * 为给定的bean工厂创建一个新的{@code ClassPathBeanDefinitionScanner}。
	 *
	 * <p>如果传入的bean工厂不仅实现了{@code BeanDefinitionRegistry}接口，而且还实现了{@code ResourceLoader}接口，
	 * 那么它将被用作默认的{@code ResourceLoader}。
	 * 这通常是{@link org.springframework.context.ApplicationContext}实现的情况。
	 *
	 * <p>如果给定一个普通的{@code BeanDefinitionRegistry}，默认的{@code ResourceLoader}
	 * 将是一个{@link org.springframework.core.io.support.PathMatchingResourcePatternResolver}。
	 *
	 * <p>如果传入的bean工厂也实现了{@link EnvironmentCapable}，那么这个阅读器将使用它的环境。
	 * 否则，阅读器将初始化并使用{@link org.springframework.core.env.StandardEnvironment}。
	 * 所有的{@code ApplicationContext}实现都是{@code EnvironmentCapable}，
	 * 而普通的{@code BeanFactory}实现则不是。
	 *
	 * @param registry 以{@code BeanDefinitionRegistry}的形式将bean定义加载到{@code BeanFactory}中
	 * @param useDefaultFilters 属性是否包含
	 * {@link org.springframework.stereotype.Component @Component}、
	 * {@link org.springframework.stereotype.Repository @Repository}、
	 * {@link org.springframework.stereotype.Service @Service}和
	 * {@link org.springframework.stereotype.Controller @Controller}的原型注解的默认过滤器
	 * @see #setResourceLoader
	 * @see #setEnvironment
	 */
	public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters) {
		this(registry, useDefaultFilters, getOrCreateEnvironment(registry));
	}

	/**
	 * 为给定bean工厂创建一个新的{@code ClassPathBeanDefinitionScanner}，
	 * 并在评估bean定义概要元数据时使用给定的{@link Environment}。
	 *
	 * <p>如果传入的bean工厂不仅实现了{@code BeanDefinitionRegistry}接口，而且还实现了{@code ResourceLoader}接口，
	 * 那么它将被用作默认的{@code ResourceLoader}。
	 * 这通常是{@link org.springframework.context.ApplicationContext}实现的情况。
	 *
	 * <p>如果给定一个普通的{@code BeanDefinitionRegistry}，默认的{@code ResourceLoader}
	 * 将是一个{@link org.springframework.core.io.support.PathMatchingResourcePatternResolver}。
	 *
	 * @param registry 以{@code BeanDefinitionRegistry}的形式将bean定义加载到{@code BeanFactory}中
	 * @param useDefaultFilters 属性是否包含
	 * {@link org.springframework.stereotype.Component @Component}、
	 * {@link org.springframework.stereotype.Repository @Repository}、
	 * {@link org.springframework.stereotype.Service @Service}和
	 * {@link org.springframework.stereotype.Controller @Controller}的原型注解的默认过滤器
	 * @param environment 评估bean定义概要文件元数据时要使用的Spring {@link Environment}
	 * @since 3.1
	 * @see #setResourceLoader
	 */
	public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters,
			Environment environment) {

		this(registry, useDefaultFilters, environment,
				(registry instanceof ResourceLoader ? (ResourceLoader) registry : null));
	}

	/**
	 * 为给定bean工厂创建一个新的{@code ClassPathBeanDefinitionScanner}，并在评估bean定义概要元数据时使用给定的{@link Environment}。
	 * @param registry 以{@code BeanDefinitionRegistry}的形式将bean定义加载到{@code BeanFactory}中
	 * @param useDefaultFilters 是否包含
	 * {@link org.springframework.stereotype.Component @Component}、
	 * {@link org.springframework.stereotype.Repository @Repository}、
	 * {@link org.springframework.stereotype.Service @Service}和
	 * {@link org.springframework.stereotype.Controller @Controller}属性的原型注解的默认过滤器
	 * @param environment 评估bean定义概要文件元数据时要使用的Spring {@link Environment}
	 * @param resourceLoader 要使用的{@link ResourceLoader}
	 * @since 4.3.6
	 */
	public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters,
			Environment environment, @Nullable ResourceLoader resourceLoader) {

		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		this.registry = registry;

		// 如果包含默认过滤器，则注册默认过滤器
		if (useDefaultFilters) {
			registerDefaultFilters();
		}
		// 设置环境
		setEnvironment(environment);
		// 设置资源加载器
		setResourceLoader(resourceLoader);
	}


	/**
	 * Return the BeanDefinitionRegistry that this scanner operates on.
	 */
	@Override
	public final BeanDefinitionRegistry getRegistry() {
		return this.registry;
	}

	/**
	 * Set the defaults to use for detected beans.
	 * @see BeanDefinitionDefaults
	 */
	public void setBeanDefinitionDefaults(@Nullable BeanDefinitionDefaults beanDefinitionDefaults) {
		this.beanDefinitionDefaults =
				(beanDefinitionDefaults != null ? beanDefinitionDefaults : new BeanDefinitionDefaults());
	}

	/**
	 * 返回检测到的bean使用的默认值(绝不是{@code null})。
	 * @since 4.1
	 */
	public BeanDefinitionDefaults getBeanDefinitionDefaults() {
		return this.beanDefinitionDefaults;
	}

	/**
	 * Set the name-matching patterns for determining autowire candidates.
	 * @param autowireCandidatePatterns the patterns to match against
	 */
	public void setAutowireCandidatePatterns(@Nullable String... autowireCandidatePatterns) {
		this.autowireCandidatePatterns = autowireCandidatePatterns;
	}

	/**
	 * Set the BeanNameGenerator to use for detected bean classes.
	 * <p>Default is a {@link AnnotationBeanNameGenerator}.
	 */
	public void setBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator =
				(beanNameGenerator != null ? beanNameGenerator : AnnotationBeanNameGenerator.INSTANCE);
	}

	/**
	 * Set the ScopeMetadataResolver to use for detected bean classes.
	 * Note that this will override any custom "scopedProxyMode" setting.
	 * <p>The default is an {@link AnnotationScopeMetadataResolver}.
	 * @see #setScopedProxyMode
	 */
	public void setScopeMetadataResolver(@Nullable ScopeMetadataResolver scopeMetadataResolver) {
		this.scopeMetadataResolver =
				(scopeMetadataResolver != null ? scopeMetadataResolver : new AnnotationScopeMetadataResolver());
	}

	/**
	 * Specify the proxy behavior for non-singleton scoped beans.
	 * Note that this will override any custom "scopeMetadataResolver" setting.
	 * <p>The default is {@link ScopedProxyMode#NO}.
	 * @see #setScopeMetadataResolver
	 */
	public void setScopedProxyMode(ScopedProxyMode scopedProxyMode) {
		this.scopeMetadataResolver = new AnnotationScopeMetadataResolver(scopedProxyMode);
	}

	/**
	 * Specify whether to register annotation config post-processors.
	 * <p>The default is to register the post-processors. Turn this off
	 * to be able to ignore the annotations or to process them differently.
	 */
	public void setIncludeAnnotationConfig(boolean includeAnnotationConfig) {
		this.includeAnnotationConfig = includeAnnotationConfig;
	}


	/**
	 * Perform a scan within the specified base packages.
	 * @param basePackages the packages to check for annotated classes
	 * @return number of beans registered
	 */
	public int scan(String... basePackages) {
		int beanCountAtScanStart = this.registry.getBeanDefinitionCount();

		doScan(basePackages);

		// Register annotation config processors, if necessary.
		if (this.includeAnnotationConfig) {
			AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
		}

		return (this.registry.getBeanDefinitionCount() - beanCountAtScanStart);
	}

	/**
	 * 在指定的基本包中执行扫描，返回注册的bean定义。
	 * <p>此方法<i>不</i>注册注释配置处理器，而是将此任务留给调用者。
	 * @param basePackages 检查带注解的类的包
	 * @return 为工具注册而注册的bean集合(如果有的话)(永远不会是 {@code null} )
	 */
	protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
		Assert.notEmpty(basePackages, "At least one base package must be specified");
		Set<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<>();
		for (String basePackage : basePackages) {
			// 在指定包下面查找候选组件
			Set<BeanDefinition> candidates = findCandidateComponents(basePackage);
			// 遍历查询到的候选组件
			for (BeanDefinition candidate : candidates) {
				// 设置范围
				ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(candidate);
				candidate.setScope(scopeMetadata.getScopeName());
				// 生成bean名称
				String beanName = this.beanNameGenerator.generateBeanName(candidate, this.registry);
				// 如果继承自AbstractBeanDefinition，则设置默认bean定义属性
				if (candidate instanceof AbstractBeanDefinition) {
					postProcessBeanDefinition((AbstractBeanDefinition) candidate, beanName);
				}
				// 如果继承自AnnotatedBeanDefinition，则处理其常用注解，使用常用注解设置对应值
				if (candidate instanceof AnnotatedBeanDefinition) {
					AnnotationConfigUtils.processCommonDefinitionAnnotations((AnnotatedBeanDefinition) candidate);
				}
				//检查给定的bean定义是否与现有的bean定义冲突
				if (checkCandidate(beanName, candidate)) {
					// 如果不冲突，则创建新的bean定义，并进行注册
					BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, beanName);
					definitionHolder =
							AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
					beanDefinitions.add(definitionHolder);
					registerBeanDefinition(definitionHolder, this.registry);
				}
			}
		}
		return beanDefinitions;
	}

	/**
	 * 如果bean定义类型实现了AbstractBeanDefinition
	 * 在扫描组件类检索到的内容之外，对给定的bean定义应用更多的设置。
	 * @param beanDefinition 扫描到的bean定义
	 * @param beanName 为给定bean生成的bean名称
	 */
	protected void postProcessBeanDefinition(AbstractBeanDefinition beanDefinition, String beanName) {
		beanDefinition.applyDefaults(this.beanDefinitionDefaults);
		if (this.autowireCandidatePatterns != null) {
			beanDefinition.setAutowireCandidate(PatternMatchUtils.simpleMatch(this.autowireCandidatePatterns, beanName));
		}
	}

	/**
	 * 使用给定的注册表注册指定的bean。
	 * <p>可以在子类中重写，例如，修改注册过程或为每个扫描bean注册进一步的bean定义。
	 * @param definitionHolder bean的定义加上bean的名称
	 * @param registry 用来注册bean的BeanDefinitionRegistry
	 */
	protected void registerBeanDefinition(BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry) {
		BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);
	}


	/**
	 * 检查给定候选bean的名称，确定是否需要注册对应的bean定义，或者是否与现有定义冲突。
	 * @param beanName bean的建议名称
	 * @param beanDefinition 对应的bean定义
	 * @return 如果bean可以按原样注册，则{@code true};{@code false}如果它应该被跳过，因为对于指定的名称有一个现有的、兼容的bean定义
	 * @throws ConflictingBeanDefinitionException 如果已为指定名称找到现有的、不兼容的bean定义
	 */
	protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) throws IllegalStateException {
		if (!this.registry.containsBeanDefinition(beanName)) {
			return true;
		}
		BeanDefinition existingDef = this.registry.getBeanDefinition(beanName);
		BeanDefinition originatingDef = existingDef.getOriginatingBeanDefinition();
		if (originatingDef != null) {
			existingDef = originatingDef;
		}
		// 返回新的bean定义是否与原有的bean定义兼容
		if (isCompatible(beanDefinition, existingDef)) {
			return false;
		}
		throw new ConflictingBeanDefinitionException("Annotation-specified bean name '" + beanName +
				"' for bean class [" + beanDefinition.getBeanClassName() + "] conflicts with existing, " +
				"non-compatible bean definition of same name and class [" + existingDef.getBeanClassName() + "]");
	}

	/**
	 * 确定给定的新bean定义是否与给定的现有bean定义兼容。
	 * <p>当现有bean定义来自同一源或非扫描源时，默认实现认为它们是兼容的。
	 * @param newDefinition 新的bean定义源于扫描
	 * @param existingDefinition 现有的bean定义，可能是显式定义的bean，也可能是以前通过扫描生成的bean
	 * @return 定义是否被认为是兼容的，新定义是否被现有定义跳过
	 */
	protected boolean isCompatible(BeanDefinition newDefinition, BeanDefinition existingDefinition) {
		return (!(existingDefinition instanceof ScannedGenericBeanDefinition) ||  // 显式注册的覆盖bean
				(newDefinition.getSource() != null && newDefinition.getSource().equals(existingDefinition.getSource())) ||  // 同一文件扫描两次
				newDefinition.equals(existingDefinition));  // 扫描等价类两次
	}


	/**
	 * Get the Environment from the given registry if possible, otherwise return a new
	 * StandardEnvironment.
	 */
	private static Environment getOrCreateEnvironment(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		if (registry instanceof EnvironmentCapable) {
			return ((EnvironmentCapable) registry).getEnvironment();
		}
		return new StandardEnvironment();
	}

}
