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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.context.annotation.DeferredImportSelector.Group;
import org.springframework.core.NestedIOException;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Parses a {@link Configuration} class definition, populating a collection of
 * {@link ConfigurationClass} objects (parsing a single Configuration class may result in
 * any number of ConfigurationClass objects because one Configuration class may import
 * another using the {@link Import} annotation).
 *
 * <p>This class helps separate the concern of parsing the structure of a Configuration
 * class from the concern of registering BeanDefinition objects based on the content of
 * that model (with the exception of {@code @ComponentScan} annotations which need to be
 * registered immediately).
 *
 * <p>This ASM-based implementation avoids reflection and eager class loading in order to
 * interoperate effectively with lazy class loading in a Spring ApplicationContext.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @since 3.0
 * @see ConfigurationClassBeanDefinitionReader
 */
class ConfigurationClassParser {

	private static final PropertySourceFactory DEFAULT_PROPERTY_SOURCE_FACTORY = new DefaultPropertySourceFactory();

	private static final Comparator<DeferredImportSelectorHolder> DEFERRED_IMPORT_COMPARATOR =
			(o1, o2) -> AnnotationAwareOrderComparator.INSTANCE.compare(o1.getImportSelector(), o2.getImportSelector());


	private final Log logger = LogFactory.getLog(getClass());

	private final MetadataReaderFactory metadataReaderFactory;

	private final ProblemReporter problemReporter;

	private final Environment environment;

	private final ResourceLoader resourceLoader;

	private final BeanDefinitionRegistry registry;

	private final ComponentScanAnnotationParser componentScanParser;

	private final ConditionEvaluator conditionEvaluator;

	private final Map<ConfigurationClass, ConfigurationClass> configurationClasses = new LinkedHashMap<>();

	private final Map<String, ConfigurationClass> knownSuperclasses = new HashMap<>();

	private final List<String> propertySourceNames = new ArrayList<>();

	private final ImportStack importStack = new ImportStack();

	private final DeferredImportSelectorHandler deferredImportSelectorHandler = new DeferredImportSelectorHandler();

	private final SourceClass objectSourceClass = new SourceClass(Object.class);


	/**
	 * 创建一个新的{@link ConfigurationClassParser}实例，用于填充一组配置类。
	 */
	public ConfigurationClassParser(MetadataReaderFactory metadataReaderFactory,
			ProblemReporter problemReporter, Environment environment, ResourceLoader resourceLoader,
			BeanNameGenerator componentScanBeanNameGenerator, BeanDefinitionRegistry registry) {

		this.metadataReaderFactory = metadataReaderFactory;
		this.problemReporter = problemReporter;
		this.environment = environment;
		this.resourceLoader = resourceLoader;
		this.registry = registry;
		this.componentScanParser = new ComponentScanAnnotationParser(
				environment, resourceLoader, componentScanBeanNameGenerator, registry);
		this.conditionEvaluator = new ConditionEvaluator(registry, environment, resourceLoader);
	}


	/**
	 * M03.05.01_1.09_01.01.05.09 遍历配置类集合，解析配置类
	 * @param configCandidates
	 */
	public void parse(Set<BeanDefinitionHolder> configCandidates) {
		// C03.05.01_1.09_01.01.05.09.01_01 遍历候选对象集合
		for (BeanDefinitionHolder holder : configCandidates) {
			BeanDefinition bd = holder.getBeanDefinition();
			try {
				// C03.05.01_1.09_01.01.05.09.01_01.01_1 如果bean定义是AnnotatedBeanDefinition，则进行配置类的处理。注册的配置类走这个方法，进行配置类的处理
				if (bd instanceof AnnotatedBeanDefinition) {
					parse(((AnnotatedBeanDefinition) bd).getMetadata(), holder.getBeanName());
				}
				// C03.05.01_1.09_01.01.05.09.01_01.01_2 如果bean定义是AbstractBeanDefinition，且内部包含@Bean方法
				else if (bd instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) bd).hasBeanClass()) {
					parse(((AbstractBeanDefinition) bd).getBeanClass(), holder.getBeanName());
				}
				// C03.05.01_1.09_01.01.05.09.01_01.01_3 其他bean定义类型的配置类解析
				else {
					parse(bd.getBeanClassName(), holder.getBeanName());
				}
			}
			catch (BeanDefinitionStoreException ex) {
				throw ex;
			}
			catch (Throwable ex) {
				throw new BeanDefinitionStoreException(
						"Failed to parse configuration class [" + bd.getBeanClassName() + "]", ex);
			}
		}

		// C03.05.01_1.09_01.01.05.09.02 处理注册的延迟导入选择器处理器
		this.deferredImportSelectorHandler.process();
	}

	/**
	 * 解析配置类
	 * @param className 类名
	 * @param beanName bean名称
	 * @throws IOException
	 */
	protected final void parse(@Nullable String className, String beanName) throws IOException {
		Assert.notNull(className, "No bean class name for configuration class bean definition");
		MetadataReader reader = this.metadataReaderFactory.getMetadataReader(className);
		processConfigurationClass(new ConfigurationClass(reader, beanName));
	}

	protected final void parse(Class<?> clazz, String beanName) throws IOException {
		processConfigurationClass(new ConfigurationClass(clazz, beanName));
	}

	/**
	 * M03.05.01_1.09_01.01.05.09.01_01.01_1 解析AnnotatedBeanDefinition定义类型的注册配置类
	 * @param metadata
	 * @param beanName
	 * @throws IOException
	 */
	protected final void parse(AnnotationMetadata metadata, String beanName) throws IOException {
		processConfigurationClass(new ConfigurationClass(metadata, beanName));
	}

	/**
	 * 校验每个{@link ConfigurationClass}对象
	 * @see ConfigurationClass#validate
	 */
	public void validate() {
		for (ConfigurationClass configClass : this.configurationClasses.keySet()) {
			configClass.validate(this.problemReporter);
		}
	}

	public Set<ConfigurationClass> getConfigurationClasses() {
		return this.configurationClasses.keySet();
	}

	/**
	 * M03.05.01_1.09_01.01.05.09.01_01.01_1.01 处理配置类
	 * @param configClass
	 * @throws IOException
	 */
	protected void processConfigurationClass(ConfigurationClass configClass) throws IOException {
		// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.01 判断是否需要跳过处理
		if (this.conditionEvaluator.shouldSkip(configClass.getMetadata(), ConfigurationPhase.PARSE_CONFIGURATION)) {
			return;
		}

		// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.02 判断给定的配置对象在配置类集合中是否存在，
		// 如果已经存在
		//     判断给定的配置对象是否是被@Import进来的，如果不是，则需要从配置类对象中移除该对象，
		//     如果是，则还需要判断已经存在的对象是否是@Import进来的，如果不是，直接返回，如果也是，则合并导入信息
		ConfigurationClass existingClass = this.configurationClasses.get(configClass);
		// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.03 如果配置类已经在配置类集合中存在
		if (existingClass != null) {
			// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.03_1 判断当前配置类是不是被@Import进来的，如果是
			if (configClass.isImported()) {
				// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.03_1_1.01 如果已存在的类是被@Import进来的，则需要将当前配置类合并到已存在的配置类中
				if (existingClass.isImported()) {
					existingClass.mergeImportedBy(configClass);
				}
				// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.03_1_1.02 直接跳过处理
				return;
			}
			else {
				// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.03_2 找到显式bean定义，可能替换导入。我们把旧的拿掉，换上新的吧。
				this.configurationClasses.remove(configClass);
				this.knownSuperclasses.values().removeIf(configClass::equals);
			}
		}

		// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.04 递归地处理配置类及其超类层次结构。
		SourceClass sourceClass = asSourceClass(configClass);
		do {
			// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1 递归处理类，返回超类进行递归处理
			sourceClass = doProcessConfigurationClass(configClass, sourceClass);
		}
		while (sourceClass != null);

		// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.06 将配置类添加到配置类集合中
		this.configurationClasses.put(configClass, configClass);
	}

	/**
	 * M03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1 通过从源类中读取注释、成员和方法，应用处理并构建一个完整的{@link ConfigurationClass}。当发现相关源时，可以多次调用此方法。
	 * @param configClass 正在构建的配置类
	 * @param sourceClass 源类
	 * @return 超类，或{@code null}(如果没有找到或以前处理过)
	 */
	@Nullable
	protected final SourceClass doProcessConfigurationClass(ConfigurationClass configClass, SourceClass sourceClass)
			throws IOException {
		// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.01 判断配置类是否添加了@Component注解，如果添加了注解，则递归处理任何成员类
		if (configClass.getMetadata().isAnnotated(Component.class.getName())) {
			// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.01_1 首先递归地处理任何成员(嵌套的)类
			processMemberClasses(configClass, sourceClass);
		}

		// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.02 处理任何@PropertySource 注解，引入属性资源
		for (AnnotationAttributes propertySource : AnnotationConfigUtils.attributesForRepeatable(
				sourceClass.getMetadata(), PropertySources.class,
				org.springframework.context.annotation.PropertySource.class)) {
			if (this.environment instanceof ConfigurableEnvironment) {
				// 处理@PropertySource注解
				processPropertySource(propertySource);
			}
			else {
				logger.info("Ignoring @PropertySource annotation on [" + sourceClass.getMetadata().getClassName() +
						"]. Reason: Environment must implement ConfigurableEnvironment");
			}
		}

		// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.03 处理任何@ComponentScan注解，执行扫描
		Set<AnnotationAttributes> componentScans = AnnotationConfigUtils.attributesForRepeatable(
				sourceClass.getMetadata(), ComponentScans.class, ComponentScan.class);
		// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.04 如果存在@ComponentScan，且无需跳过处理该类
		if (!componentScans.isEmpty() &&
				!this.conditionEvaluator.shouldSkip(sourceClass.getMetadata(), ConfigurationPhase.REGISTER_BEAN)) {
			// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.04_01 遍历配置的@ComponentScan注解，执行扫描
			for (AnnotationAttributes componentScan : componentScans) {
				// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.04_01.01 配置类由@ComponentScan注解——>立即执行扫描
				Set<BeanDefinitionHolder> scannedBeanDefinitions =
						this.componentScanParser.parse(componentScan, sourceClass.getMetadata().getClassName());
				// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.04_01.02 遍历检查已扫描的定义集以获得更多的配置类，并在需要时进行递归解析
				for (BeanDefinitionHolder holder : scannedBeanDefinitions) {
					// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.04_01.02_01.01 得到原始bean定义，如果原始bean定义为null，则使用当前bean定义
					BeanDefinition bdCand = holder.getBeanDefinition().getOriginatingBeanDefinition();
					if (bdCand == null) {
						bdCand = holder.getBeanDefinition();
					}
					// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.04_01.02_01.02
					if (ConfigurationClassUtils.checkConfigurationClassCandidate(bdCand, this.metadataReaderFactory)) {
						parse(bdCand.getBeanClassName(), holder.getBeanName());
					}
				}
			}
		}

		// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.05 处理任何@Import注解
		processImports(configClass, sourceClass, getImports(sourceClass), true);

		// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.06 读取@ImportResource注解内容
		AnnotationAttributes importResource =
				AnnotationConfigUtils.attributesFor(sourceClass.getMetadata(), ImportResource.class);
		// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.07 如果添加了@ImportResource注解，则获取参数值遍历处理
		if (importResource != null) {
			// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.07_1.01 读取 locations 和 reader参数值
			String[] resources = importResource.getStringArray("locations");
			Class<? extends BeanDefinitionReader> readerClass = importResource.getClass("reader");
			// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.07_1.02 遍历locations属性值，导入资源
			for (String resource : resources) {
				String resolvedResource = this.environment.resolveRequiredPlaceholders(resource);
				configClass.addImportedResource(resolvedResource, readerClass);
			}
		}

		// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.08 获取使用了@Bean注解的方法元数据，处理单个@Bean方法
		Set<MethodMetadata> beanMethods = retrieveBeanMethodMetadata(sourceClass);
		// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.09 将读取到的@Bean方法保存到配置类的Bean方法中
		for (MethodMetadata methodMetadata : beanMethods) {
			configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));
		}

		// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.10 处理接口上的默认方法
		processInterfaces(configClass, sourceClass);

		// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.11 如果有的话，处理超类
		if (sourceClass.getMetadata().hasSuperClass()) {
			// 得到超类名称
			String superclass = sourceClass.getMetadata().getSuperClassName();
			// 如果超类不是JDK的内置类，并且，在已知的超类集合中不存在，则将该类添加到已知的超类集合中
			if (superclass != null && !superclass.startsWith("java") &&
					!this.knownSuperclasses.containsKey(superclass)) {
				this.knownSuperclasses.put(superclass, configClass);
				// 找到超类，返回其注释元数据并递归
				return sourceClass.getSuperClass();
			}
		}

		// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.12 没有超类 -> 处理完成
		return null;
	}

	/**
	 * M03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.01_1 注册碰巧是配置类本身的成员(嵌套的)类。
	 */
	private void processMemberClasses(ConfigurationClass configClass, SourceClass sourceClass) throws IOException {
		Collection<SourceClass> memberClasses = sourceClass.getMemberClasses();
		if (!memberClasses.isEmpty()) {
			List<SourceClass> candidates = new ArrayList<>(memberClasses.size());
			for (SourceClass memberClass : memberClasses) {
				if (ConfigurationClassUtils.isConfigurationCandidate(memberClass.getMetadata()) &&
						!memberClass.getMetadata().getClassName().equals(configClass.getMetadata().getClassName())) {
					candidates.add(memberClass);
				}
			}
			OrderComparator.sort(candidates);
			for (SourceClass candidate : candidates) {
				if (this.importStack.contains(configClass)) {
					this.problemReporter.error(new CircularImportProblem(configClass, this.importStack));
				}
				else {
					this.importStack.push(configClass);
					try {
						processConfigurationClass(candidate.asConfigClass(configClass));
					}
					finally {
						this.importStack.pop();
					}
				}
			}
		}
	}

	/**
	 * 在配置类实现的接口上注册默认方法。
	 */
	private void processInterfaces(ConfigurationClass configClass, SourceClass sourceClass) throws IOException {
		// 遍历源类的接口类
		for (SourceClass ifc : sourceClass.getInterfaces()) {
			// 获取接口类中使用@Bean注解的方法集合
			Set<MethodMetadata> beanMethods = retrieveBeanMethodMetadata(ifc);
			// 遍历方法集合
			for (MethodMetadata methodMetadata : beanMethods) {
				// 如果方法不是抽象方法
				if (!methodMetadata.isAbstract()) {
					// Java 8+接口上的默认方法或其他具体方法…
					configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));
				}
			}
			// 递归遍历上级接口
			processInterfaces(configClass, ifc);
		}
	}

	/**
	 * M03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.08 检索所有<code>@Bean</code>方法的元数据。
	 */
	private Set<MethodMetadata> retrieveBeanMethodMetadata(SourceClass sourceClass) {
		// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.08.01 得到配置类的元数据
		AnnotationMetadata original = sourceClass.getMetadata();
		// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.08.02 获取添加了@Bean注解的方法元数据集合
		Set<MethodMetadata> beanMethods = original.getAnnotatedMethods(Bean.class.getName());
		// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.08.03 如果存在@Bean注解的方法
		if (beanMethods.size() > 1 && original instanceof StandardAnnotationMetadata) {
			try {
				// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.08.03_1.01 尝试通过ASM读取类文件以确定声明顺序…
				// 不幸的是，JVM的标准反射以任意顺序返回方法，甚至在相同JVM上运行相同应用程序的不同时间之间也是如此。
				AnnotationMetadata asm =
						this.metadataReaderFactory.getMetadataReader(original.getClassName()).getAnnotationMetadata();
				// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.08.03_1.02 使用ASM方式获取@Bean注解的方法元数据
				Set<MethodMetadata> asmMethods = asm.getAnnotatedMethods(Bean.class.getName());
				// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.08.03_1.03_1 如果使用ASM方式获取到的注解方法数量大于直接获取的注解方法数量
				if (asmMethods.size() >= beanMethods.size()) {
					// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.08.03_1.03_1.01 则通过循环比较，添加使用注解方法直接获取到的方法
					Set<MethodMetadata> selectedMethods = new LinkedHashSet<>(asmMethods.size());
					for (MethodMetadata asmMethod : asmMethods) {
						for (MethodMetadata beanMethod : beanMethods) {
							if (beanMethod.getMethodName().equals(asmMethod.getMethodName())) {
								selectedMethods.add(beanMethod);
								break;
							}
						}
					}
					// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.08.03_1.03_1.02 如果ASM方法获取的方法数量包含所有使用注解直接获取的方法数量
					if (selectedMethods.size() == beanMethods.size()) {
						// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.08.03_1.03_1.02_1 在ASM方法集中找到的所有反射检测方法 -> 继续执行
						beanMethods = selectedMethods;
					}
				}
			}
			catch (IOException ex) {
				logger.debug("Failed to read class file via ASM for determining @Bean method order", ex);
				// No worries, let's continue with the reflection metadata we started with...
			}
		}
		// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.08.04 返回获取到的类中的@Bean注解的方法
		return beanMethods;
	}


	/**
	 * 处理给定的<code>@PropertySource</code>注解元数据
	 * @param propertySource <code>@PropertySource</code>注解找到的元数据
	 * @throws IOException 如果加载属性源失败
	 */
	private void processPropertySource(AnnotationAttributes propertySource) throws IOException {
		String name = propertySource.getString("name");
		if (!StringUtils.hasLength(name)) {
			name = null;
		}
		String encoding = propertySource.getString("encoding");
		if (!StringUtils.hasLength(encoding)) {
			encoding = null;
		}
		// 获取文件位置的值配置
		String[] locations = propertySource.getStringArray("value");
		Assert.isTrue(locations.length > 0, "At least one @PropertySource(value) location is required");
		// 判断是否添加了如果资源找不到就忽略的标志
		boolean ignoreResourceNotFound = propertySource.getBoolean("ignoreResourceNotFound");

		Class<? extends PropertySourceFactory> factoryClass = propertySource.getClass("factory");
		PropertySourceFactory factory = (factoryClass == PropertySourceFactory.class ?
				DEFAULT_PROPERTY_SOURCE_FACTORY : BeanUtils.instantiateClass(factoryClass));

		for (String location : locations) {
			try {
				String resolvedLocation = this.environment.resolveRequiredPlaceholders(location);
				Resource resource = this.resourceLoader.getResource(resolvedLocation);
				addPropertySource(factory.createPropertySource(name, new EncodedResource(resource, encoding)));
			}
			catch (IllegalArgumentException | FileNotFoundException | UnknownHostException ex) {
				// Placeholders not resolvable or resource not found when trying to open it
				if (ignoreResourceNotFound) {
					if (logger.isInfoEnabled()) {
						logger.info("Properties location [" + location + "] not resolvable: " + ex.getMessage());
					}
				}
				else {
					throw ex;
				}
			}
		}
	}

	/**
	 * 添加属性源
	 * @param propertySource
	 */
	private void addPropertySource(PropertySource<?> propertySource) {
		String name = propertySource.getName();
		MutablePropertySources propertySources = ((ConfigurableEnvironment) this.environment).getPropertySources();

		if (this.propertySourceNames.contains(name)) {
			// 我们已经添加了一个版本，我们需要扩展它
			PropertySource<?> existing = propertySources.get(name);
			if (existing != null) {
				PropertySource<?> newSource = (propertySource instanceof ResourcePropertySource ?
						((ResourcePropertySource) propertySource).withResourceName() : propertySource);
				if (existing instanceof CompositePropertySource) {
					((CompositePropertySource) existing).addFirstPropertySource(newSource);
				}
				else {
					if (existing instanceof ResourcePropertySource) {
						existing = ((ResourcePropertySource) existing).withResourceName();
					}
					CompositePropertySource composite = new CompositePropertySource(name);
					composite.addPropertySource(newSource);
					composite.addPropertySource(existing);
					propertySources.replace(name, composite);
				}
				return;
			}
		}

		if (this.propertySourceNames.isEmpty()) {
			propertySources.addLast(propertySource);
		}
		else {
			String firstProcessed = this.propertySourceNames.get(this.propertySourceNames.size() - 1);
			propertySources.addBefore(firstProcessed, propertySource);
		}
		this.propertySourceNames.add(name);
	}


	/**
	 * 考虑到所有元注释，返回{@code @Import}类。
	 */
	private Set<SourceClass> getImports(SourceClass sourceClass) throws IOException {
		Set<SourceClass> imports = new LinkedHashSet<>();
		Set<SourceClass> visited = new LinkedHashSet<>();
		// 递归遍历注解值，获取所有@Import注解的属性值
		collectImports(sourceClass, imports, visited);
		return imports;
	}

	/**
	 * 递归地收集所有声明的{@code @Import}值。
	 * 与大多数元注释不同的是，使用不同的值声明几个{@code @Import}是有效的;
	 * 通常从类的第一个元注释返回值的过程是不够的。
	 * <p>例如，除了源自{@code @Enable}注释的元导入外，
	 * {@code @Configuration}类通常还声明直接的{@code @Import}。
	 * @param sourceClass 要搜索的类
	 * @param imports 截止当前收集到的imports
	 * @param visited 用于跟踪已访问的类，以防止无限递归
	 * @throws IOException 如果从命名类读取元数据有任何问题
	 */
	private void collectImports(SourceClass sourceClass, Set<SourceClass> imports, Set<SourceClass> visited)
			throws IOException {
		// 如果源类没有进行过处理，则在添加到集合中时会返回true，否则会返回false
		if (visited.add(sourceClass)) {
			for (SourceClass annotation : sourceClass.getAnnotations()) {
				String annName = annotation.getMetadata().getClassName();
				// 如果注解类的类名不是@Import，则继续递归搜索元注解
				if (!annName.equals(Import.class.getName())) {
					collectImports(annotation, imports, visited);
				}
			}
			// 得到@Import注解属性值，添加到收集的imports集合中
			imports.addAll(sourceClass.getAnnotationAttributes(Import.class.getName(), "value"));
		}
	}

	/**
	 * M03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.05 处理任何@Import注解
	 * @param configClass 正在构建的配置类
	 * @param currentSourceClass 源类
	 * @param importCandidates 递归获取到的所有@Import注解值
	 * @param checkForCircularImports 检查循环导入
	 */
	private void processImports(ConfigurationClass configClass, SourceClass currentSourceClass,
			Collection<SourceClass> importCandidates, boolean checkForCircularImports) {
		// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.05.01 如果没有指定任何的@Import，则直接返回
		if (importCandidates.isEmpty()) {
			return;
		}

		// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.05.02_1 如果检查循环导入，且使用了栈的链式导入，则记录循环导入错误
		if (checkForCircularImports && isChainedImportOnStack(configClass)) {
			this.problemReporter.error(new CircularImportProblem(configClass, this.importStack));
		}
		else {
			// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.05.02_2.01 将配置类放入栈顶
			this.importStack.push(configClass);
			try {
				// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.05.02_2.02 遍历@Import导入的属性值
				for (SourceClass candidate : importCandidates) {
					// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.05.02_2.02_01.01 判断属性值中的类是否是ImportSelector接口的实现类
					if (candidate.isAssignable(ImportSelector.class)) {
						// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.05.02_2.02_01.01_1.01 候选类是一个ImportSelector ->委托给它来决定导入
						Class<?> candidateClass = candidate.loadClass();
						// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.05.02_2.02_01.01_1.02 使用候选类的对象来构建一个ImportSelector
						ImportSelector selector = BeanUtils.instantiateClass(candidateClass, ImportSelector.class);
						ParserStrategyUtils.invokeAwareMethods(
								selector, this.environment, this.resourceLoader, this.registry);
						// C03.05.01_1.09_01.01.05.09.01_01.01_1.01.05-1.05.02_2.02_01.01_1.03_1 如果导入选择器实现了DeferredImportSelector接口
						if (selector instanceof DeferredImportSelector) {
							this.deferredImportSelectorHandler.handle(configClass, (DeferredImportSelector) selector);
						}
						else {
							// 获取要导入的类名列表
							String[] importClassNames = selector.selectImports(currentSourceClass.getMetadata());
							Collection<SourceClass> importSourceClasses = asSourceClasses(importClassNames);
							// 处理进行导入，递归处理
							processImports(configClass, currentSourceClass, importSourceClasses, false);
						}
					}
					// 如果实现了ImportBeanDefinitionRegistrar接口
					else if (candidate.isAssignable(ImportBeanDefinitionRegistrar.class)) {
						// 候选类是一个ImportBeanDefinitionRegistrar ->委托它用于注册其他bean定义
						Class<?> candidateClass = candidate.loadClass();
						// 依照候选类，构建一个ImportBeanDefinitionRegistrar对象
						ImportBeanDefinitionRegistrar registrar =
								BeanUtils.instantiateClass(candidateClass, ImportBeanDefinitionRegistrar.class);
						ParserStrategyUtils.invokeAwareMethods(
								registrar, this.environment, this.resourceLoader, this.registry);
						// 将注册器和元数据添加到注册器集合中
						configClass.addImportBeanDefinitionRegistrar(registrar, currentSourceClass.getMetadata());
					}
					else {
						// 候选类不是ImportSelector或ImportBeanDefinitionRegistrar—>将其作为@Configuration类处理
						this.importStack.registerImport(
								currentSourceClass.getMetadata(), candidate.getMetadata().getClassName());
						// 处理配置类
						processConfigurationClass(candidate.asConfigClass(configClass));
					}
				}
			}
			catch (BeanDefinitionStoreException ex) {
				throw ex;
			}
			catch (Throwable ex) {
				throw new BeanDefinitionStoreException(
						"Failed to process import candidates for configuration class [" +
						configClass.getMetadata().getClassName() + "]", ex);
			}
			finally {
				// 移除栈顶元素
				this.importStack.pop();
			}
		}
	}

	/**
	 * 判断是否在栈中链式导入
	 * @param configClass 正在构建的配置类
	 * @return 如果使用的是导入栈，且栈中存在正在构建的配置类，则返回true；否则返回false
	 */
	private boolean isChainedImportOnStack(ConfigurationClass configClass) {
		if (this.importStack.contains(configClass)) {
			String configClassName = configClass.getMetadata().getClassName();
			AnnotationMetadata importingClass = this.importStack.getImportingClassFor(configClassName);
			while (importingClass != null) {
				if (configClassName.equals(importingClass.getClassName())) {
					return true;
				}
				importingClass = this.importStack.getImportingClassFor(importingClass.getClassName());
			}
		}
		return false;
	}

	ImportRegistry getImportRegistry() {
		return this.importStack;
	}


	/**
	 * 从{@link ConfigurationClass}获取{@link SourceClass}的工厂方法。
	 */
	private SourceClass asSourceClass(ConfigurationClass configurationClass) throws IOException {
		AnnotationMetadata metadata = configurationClass.getMetadata();
		if (metadata instanceof StandardAnnotationMetadata) {
			return asSourceClass(((StandardAnnotationMetadata) metadata).getIntrospectedClass());
		}
		return asSourceClass(metadata.getClassName());
	}

	/**
	 * Factory method to obtain a {@link SourceClass} from a {@link Class}.
	 */
	SourceClass asSourceClass(@Nullable Class<?> classType) throws IOException {
		if (classType == null || classType.getName().startsWith("java.lang.annotation.")) {
			return this.objectSourceClass;
		}
		try {
			// Sanity test that we can reflectively read annotations,
			// including Class attributes; if not -> fall back to ASM
			for (Annotation ann : classType.getDeclaredAnnotations()) {
				AnnotationUtils.validateAnnotation(ann);
			}
			return new SourceClass(classType);
		}
		catch (Throwable ex) {
			// Enforce ASM via class name resolution
			return asSourceClass(classType.getName());
		}
	}

	/**
	 * 从类名中获取{@link SourceClass SourceClasss}的工厂方法。
	 */
	private Collection<SourceClass> asSourceClasses(String... classNames) throws IOException {
		List<SourceClass> annotatedClasses = new ArrayList<>(classNames.length);
		for (String className : classNames) {
			annotatedClasses.add(asSourceClass(className));
		}
		return annotatedClasses;
	}

	/**
	 * 工厂方法从类名获取{@link SourceClass}。
	 */
	SourceClass asSourceClass(@Nullable String className) throws IOException {
		if (className == null || className.startsWith("java.lang.annotation.")) {
			return this.objectSourceClass;
		}
		if (className.startsWith("java")) {
			// 不要对核心java类型使用ASM
			try {
				return new SourceClass(ClassUtils.forName(className,
						this.resourceLoader.getClassLoader()));
			}
			catch (ClassNotFoundException ex) {
				throw new NestedIOException(
						"Failed to load class [" + className + "]", ex);
			}
		}
		return new SourceClass(
				this.metadataReaderFactory.getMetadataReader(className));
	}


	@SuppressWarnings("serial")
	private static class ImportStack extends ArrayDeque<ConfigurationClass> implements ImportRegistry {

		private final MultiValueMap<String, AnnotationMetadata> imports = new LinkedMultiValueMap<>();

		public void registerImport(AnnotationMetadata importingClass, String importedClass) {
			this.imports.add(importedClass, importingClass);
		}

		@Override
		@Nullable
		public AnnotationMetadata getImportingClassFor(String importedClass) {
			return CollectionUtils.lastElement(this.imports.get(importedClass));
		}

		@Override
		public void removeImportingClass(String importingClass) {
			for (List<AnnotationMetadata> list : this.imports.values()) {
				for (Iterator<AnnotationMetadata> iterator = list.iterator(); iterator.hasNext();) {
					if (iterator.next().getClassName().equals(importingClass)) {
						iterator.remove();
						break;
					}
				}
			}
		}

		/**
		 * Given a stack containing (in order)
		 * <ul>
		 * <li>com.acme.Foo</li>
		 * <li>com.acme.Bar</li>
		 * <li>com.acme.Baz</li>
		 * </ul>
		 * return "[Foo->Bar->Baz]".
		 */
		@Override
		public String toString() {
			StringJoiner joiner = new StringJoiner("->", "[", "]");
			for (ConfigurationClass configurationClass : this) {
				joiner.add(configurationClass.getSimpleName());
			}
			return joiner.toString();
		}
	}


	private class DeferredImportSelectorHandler {

		@Nullable
		private List<DeferredImportSelectorHolder> deferredImportSelectors = new ArrayList<>();

		/**
		 * 处理指定的{@link DeferredImportSelector}。
		 * 如果正在收集延迟导入选择器，则会将此实例注册到列表中。
		 * 如果正在处理它们，{@link DeferredImportSelector}也会根据它的{@link DeferredImportSelector.Group}进行处理。
		 * @param configClass 源配置类
		 * @param importSelector 要处理的选择器
		 */
		public void handle(ConfigurationClass configClass, DeferredImportSelector importSelector) {
			DeferredImportSelectorHolder holder = new DeferredImportSelectorHolder(
					configClass, importSelector);
			if (this.deferredImportSelectors == null) {
				DeferredImportSelectorGroupingHandler handler = new DeferredImportSelectorGroupingHandler();
				handler.register(holder);
				handler.processGroupImports();
			}
			else {
				this.deferredImportSelectors.add(holder);
			}
		}

		/**
		 * 进行导入选择器的处理
		 */
		public void process() {
			List<DeferredImportSelectorHolder> deferredImports = this.deferredImportSelectors;
			// 进入处理，置空集合
			this.deferredImportSelectors = null;
			try {
				// 如果有延迟导入处理器
				if (deferredImports != null) {
					DeferredImportSelectorGroupingHandler handler = new DeferredImportSelectorGroupingHandler();
					deferredImports.sort(DEFERRED_IMPORT_COMPARATOR);
					deferredImports.forEach(handler::register);
					handler.processGroupImports();
				}
			}
			finally {
				this.deferredImportSelectors = new ArrayList<>();
			}
		}

	}


	private class DeferredImportSelectorGroupingHandler {

		private final Map<Object, DeferredImportSelectorGrouping> groupings = new LinkedHashMap<>();

		private final Map<AnnotationMetadata, ConfigurationClass> configurationClasses = new HashMap<>();

		/**
		 * 进行注册
		 * @param deferredImport
		 */
		public void register(DeferredImportSelectorHolder deferredImport) {
			Class<? extends Group> group = deferredImport.getImportSelector()
					.getImportGroup();
			DeferredImportSelectorGrouping grouping = this.groupings.computeIfAbsent(
					(group != null ? group : deferredImport),
					key -> new DeferredImportSelectorGrouping(createGroup(group)));
			grouping.add(deferredImport);
			this.configurationClasses.put(deferredImport.getConfigurationClass().getMetadata(),
					deferredImport.getConfigurationClass());
		}

		/**
		 * 处理组导入
		 */
		public void processGroupImports() {
			for (DeferredImportSelectorGrouping grouping : this.groupings.values()) {
				grouping.getImports().forEach(entry -> {
					ConfigurationClass configurationClass = this.configurationClasses.get(
							entry.getMetadata());
					try {
						processImports(configurationClass, asSourceClass(configurationClass),
								asSourceClasses(entry.getImportClassName()), false);
					}
					catch (BeanDefinitionStoreException ex) {
						throw ex;
					}
					catch (Throwable ex) {
						throw new BeanDefinitionStoreException(
								"Failed to process import candidates for configuration class [" +
										configurationClass.getMetadata().getClassName() + "]", ex);
					}
				});
			}
		}

		private Group createGroup(@Nullable Class<? extends Group> type) {
			Class<? extends Group> effectiveType = (type != null ? type
					: DefaultDeferredImportSelectorGroup.class);
			Group group = BeanUtils.instantiateClass(effectiveType);
			ParserStrategyUtils.invokeAwareMethods(group,
					ConfigurationClassParser.this.environment,
					ConfigurationClassParser.this.resourceLoader,
					ConfigurationClassParser.this.registry);
			return group;
		}

	}


	private static class DeferredImportSelectorHolder {

		private final ConfigurationClass configurationClass;

		private final DeferredImportSelector importSelector;

		public DeferredImportSelectorHolder(ConfigurationClass configClass, DeferredImportSelector selector) {
			this.configurationClass = configClass;
			this.importSelector = selector;
		}

		public ConfigurationClass getConfigurationClass() {
			return this.configurationClass;
		}

		public DeferredImportSelector getImportSelector() {
			return this.importSelector;
		}
	}


	private static class DeferredImportSelectorGrouping {

		private final DeferredImportSelector.Group group;

		private final List<DeferredImportSelectorHolder> deferredImports = new ArrayList<>();

		DeferredImportSelectorGrouping(Group group) {
			this.group = group;
		}

		public void add(DeferredImportSelectorHolder deferredImport) {
			this.deferredImports.add(deferredImport);
		}

		/**
		 * Return the imports defined by the group.
		 * @return each import with its associated configuration class
		 */
		public Iterable<Group.Entry> getImports() {
			for (DeferredImportSelectorHolder deferredImport : this.deferredImports) {
				this.group.process(deferredImport.getConfigurationClass().getMetadata(),
						deferredImport.getImportSelector());
			}
			return this.group.selectImports();
		}
	}


	private static class DefaultDeferredImportSelectorGroup implements Group {

		private final List<Entry> imports = new ArrayList<>();

		@Override
		public void process(AnnotationMetadata metadata, DeferredImportSelector selector) {
			for (String importClassName : selector.selectImports(metadata)) {
				this.imports.add(new Entry(metadata, importClassName));
			}
		}

		@Override
		public Iterable<Entry> selectImports() {
			return this.imports;
		}
	}


	/**
	 * Simple wrapper that allows annotated source classes to be dealt with
	 * in a uniform manner, regardless of how they are loaded.
	 */
	private class SourceClass implements Ordered {

		private final Object source;  // Class or MetadataReader

		private final AnnotationMetadata metadata;

		public SourceClass(Object source) {
			this.source = source;
			if (source instanceof Class) {
				this.metadata = AnnotationMetadata.introspect((Class<?>) source);
			}
			else {
				this.metadata = ((MetadataReader) source).getAnnotationMetadata();
			}
		}

		public final AnnotationMetadata getMetadata() {
			return this.metadata;
		}

		@Override
		public int getOrder() {
			Integer order = ConfigurationClassUtils.getOrder(this.metadata);
			return (order != null ? order : Ordered.LOWEST_PRECEDENCE);
		}

		public Class<?> loadClass() throws ClassNotFoundException {
			if (this.source instanceof Class) {
				return (Class<?>) this.source;
			}
			String className = ((MetadataReader) this.source).getClassMetadata().getClassName();
			return ClassUtils.forName(className, resourceLoader.getClassLoader());
		}

		public boolean isAssignable(Class<?> clazz) throws IOException {
			if (this.source instanceof Class) {
				return clazz.isAssignableFrom((Class<?>) this.source);
			}
			return new AssignableTypeFilter(clazz).match((MetadataReader) this.source, metadataReaderFactory);
		}

		public ConfigurationClass asConfigClass(ConfigurationClass importedBy) {
			if (this.source instanceof Class) {
				return new ConfigurationClass((Class<?>) this.source, importedBy);
			}
			return new ConfigurationClass((MetadataReader) this.source, importedBy);
		}

		public Collection<SourceClass> getMemberClasses() throws IOException {
			Object sourceToProcess = this.source;
			if (sourceToProcess instanceof Class) {
				Class<?> sourceClass = (Class<?>) sourceToProcess;
				try {
					Class<?>[] declaredClasses = sourceClass.getDeclaredClasses();
					List<SourceClass> members = new ArrayList<>(declaredClasses.length);
					for (Class<?> declaredClass : declaredClasses) {
						members.add(asSourceClass(declaredClass));
					}
					return members;
				}
				catch (NoClassDefFoundError err) {
					// getDeclaredClasses() failed because of non-resolvable dependencies
					// -> fall back to ASM below
					sourceToProcess = metadataReaderFactory.getMetadataReader(sourceClass.getName());
				}
			}

			// ASM-based resolution - safe for non-resolvable classes as well
			MetadataReader sourceReader = (MetadataReader) sourceToProcess;
			String[] memberClassNames = sourceReader.getClassMetadata().getMemberClassNames();
			List<SourceClass> members = new ArrayList<>(memberClassNames.length);
			for (String memberClassName : memberClassNames) {
				try {
					members.add(asSourceClass(memberClassName));
				}
				catch (IOException ex) {
					// Let's skip it if it's not resolvable - we're just looking for candidates
					if (logger.isDebugEnabled()) {
						logger.debug("Failed to resolve member class [" + memberClassName +
								"] - not considering it as a configuration class candidate");
					}
				}
			}
			return members;
		}

		public SourceClass getSuperClass() throws IOException {
			if (this.source instanceof Class) {
				return asSourceClass(((Class<?>) this.source).getSuperclass());
			}
			return asSourceClass(((MetadataReader) this.source).getClassMetadata().getSuperClassName());
		}

		public Set<SourceClass> getInterfaces() throws IOException {
			Set<SourceClass> result = new LinkedHashSet<>();
			if (this.source instanceof Class) {
				Class<?> sourceClass = (Class<?>) this.source;
				for (Class<?> ifcClass : sourceClass.getInterfaces()) {
					result.add(asSourceClass(ifcClass));
				}
			}
			else {
				for (String className : this.metadata.getInterfaceNames()) {
					result.add(asSourceClass(className));
				}
			}
			return result;
		}

		public Set<SourceClass> getAnnotations() {
			Set<SourceClass> result = new LinkedHashSet<>();
			if (this.source instanceof Class) {
				Class<?> sourceClass = (Class<?>) this.source;
				for (Annotation ann : sourceClass.getDeclaredAnnotations()) {
					Class<?> annType = ann.annotationType();
					if (!annType.getName().startsWith("java")) {
						try {
							result.add(asSourceClass(annType));
						}
						catch (Throwable ex) {
							// An annotation not present on the classpath is being ignored
							// by the JVM's class loading -> ignore here as well.
						}
					}
				}
			}
			else {
				for (String className : this.metadata.getAnnotationTypes()) {
					if (!className.startsWith("java")) {
						try {
							result.add(getRelated(className));
						}
						catch (Throwable ex) {
							// An annotation not present on the classpath is being ignored
							// by the JVM's class loading -> ignore here as well.
						}
					}
				}
			}
			return result;
		}

		public Collection<SourceClass> getAnnotationAttributes(String annType, String attribute) throws IOException {
			Map<String, Object> annotationAttributes = this.metadata.getAnnotationAttributes(annType, true);
			if (annotationAttributes == null || !annotationAttributes.containsKey(attribute)) {
				return Collections.emptySet();
			}
			String[] classNames = (String[]) annotationAttributes.get(attribute);
			Set<SourceClass> result = new LinkedHashSet<>();
			for (String className : classNames) {
				result.add(getRelated(className));
			}
			return result;
		}

		private SourceClass getRelated(String className) throws IOException {
			if (this.source instanceof Class) {
				try {
					Class<?> clazz = ClassUtils.forName(className, ((Class<?>) this.source).getClassLoader());
					return asSourceClass(clazz);
				}
				catch (ClassNotFoundException ex) {
					// Ignore -> fall back to ASM next, except for core java types.
					if (className.startsWith("java")) {
						throw new NestedIOException("Failed to load class [" + className + "]", ex);
					}
					return new SourceClass(metadataReaderFactory.getMetadataReader(className));
				}
			}
			return asSourceClass(className);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof SourceClass &&
					this.metadata.getClassName().equals(((SourceClass) other).metadata.getClassName())));
		}

		@Override
		public int hashCode() {
			return this.metadata.getClassName().hashCode();
		}

		@Override
		public String toString() {
			return this.metadata.getClassName();
		}
	}


	/**
	 * {@link Problem}在检测到循环{@link Import}时注册。
	 */
	private static class CircularImportProblem extends Problem {

		/**
		 * 返回循环导入异常对象
		 * @param attemptedImport
		 * @param importStack
		 */
		public CircularImportProblem(ConfigurationClass attemptedImport, Deque<ConfigurationClass> importStack) {
			super(String.format("A circular @Import has been detected: " +
					"Illegal attempt by @Configuration class '%s' to import class '%s' as '%s' is " +
					"already present in the current import stack %s", importStack.element().getSimpleName(),
					attemptedImport.getSimpleName(), attemptedImport.getSimpleName(), importStack),
					new Location(importStack.element().getResource(), attemptedImport.getMetadata()));
		}
	}

}
