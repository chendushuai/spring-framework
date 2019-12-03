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

package org.springframework.context.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.Nullable;

/**
 * 委托AbstractApplicationContext的后处理器处理。
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
final class PostProcessorRegistrationDelegate {

	private PostProcessorRegistrationDelegate() {
	}

	/**
	 * M03.05.01 请求BeanFactoryPostProcessor集合处理
	 *
	 * beanFactoryPostProcessors
	 * 1. 没有元素
	 * 2. 有元素。 来自于程序员提供的BeanFactoryPostProcessor
	 * 		addBeanFactoryPostProcessor
	 * @param beanFactory bean工厂
	 * @param beanFactoryPostProcessors bean工厂后置处理器集合
	 */
	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// C03.05.01.01 已经处理的bean集合。如果有的话，首先调用BeanDefinitionRegistryPostProcessor。
		Set<String> processedBeans = new HashSet<>();

		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			// C03.05.01.02 常规后置处理器
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			// C03.05.01.03 已处理的bean定义注册后置处理器
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();

			// C03.05.01.04 遍历给定的bean工厂后置处理器集合beanFactoryPostProcessors，这里的后置处理器集合来源为上下文对象
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				// C03.05.01.04_1 如果后置处理器的类型是bean定义注册后置处理器BeanDefinitionRegistryPostProcessor，调用其postProcessBeanDefinitionRegistry方法
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					// 有可能给定的对象的类型是BeanDefinitionRegistryPostProcessor的子类
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					// 在上下文初始化，bean定义加载完成，但尚未初始化之前，执行处理
					// C03.05.01.04_1_1 执行自定义的BeanDefinitionRegistryPostProcessor接口的实现后置处理器的postProcessBeanDefinitionRegistry方法，
					// 通过org.springframework.context.support.AbstractApplicationContext.addBeanFactoryPostProcessor
					// 注册到后置处理器列表中后，就会在此处进行调用postProcessBeanDefinitionRegistry方法，执行bean定义注册处理。
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					// C03.05.01.04_1_2 将已处理的BeanDefinitionRegistryPostProcessor后置处理器保存到已处理已处理的bean定义注册后置处理器集合中
					registryProcessors.add(registryProcessor);
				}
				else {
					// C03.05.01.04_1 否则将其保存到常规后置处理器集合中
					regularPostProcessors.add(postProcessor);
				}
			}

			// 不要在这里初始化FactoryBean：
			// 我们需要不初始化所有常规bean，让bean工厂的后处理程序应用于它们！
			// 将实现优先排序、有序的BeanDefinitionRegistryPostProcessor与其他处理器分开。
			// 这个currentRegistryProcessors 放的是spring内部自己实现了BeanDefinitionRegistryPostProcessor接口的对象。
			// 或者是自己注册的后置处理器
			// C03.05.01.05 当前正在处理的后置处理器集合
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// 首先，调用实现PriorityOrdered的BeanDefinitionRegistryPostProcessors。
			// BeanDefinitionRegistryPostProcessor 等于 BeanFactoryPostProcessor
			// getBeanNamesForType 根据bean的类型获取bean的名字ConfigurationClassPostProcessor

			// 在这里进行了第一次的bean定义的合并，为了获取完整的合并后的bean定义，然后再进行筛选
			// C03.05.01.06 根据BeanDefinitionRegistryPostProcessor类型获取bean定义对应的名称列表集合
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			/**
			 * 这个地方可以得到一个BeanFactoryPostProcessor，因为是spring默认在最开始自己注册的
			 * 为什么要在最开始注册这个呢？
			 * 因为spring的工厂需要许解析去扫描等等功能
			 * 而这些功能都是需要在spring工厂初始化完成之前执行
			 * 要么在工厂最开始的时候、要么在工厂初始化之中，反正不能再之后
			 * 因为如果在之后就没有意义，因为那个时候已经需要使用工厂了
			 * 所以这里spring'在一开始就注册了一个BeanFactoryPostProcessor，用来插手springfactory的实例化过程
			 * 在这个地方断点可以知道这个类叫做ConfigurationClassPostProcessor
			 * ConfigurationClassPostProcessor那么这个类能干嘛呢？可以参考源码
			 * 下面我们对这个牛逼哄哄的类（他能插手spring工厂的实例化过程还不牛逼吗？）重点解释
			 */
			// C03.05.01.07 遍历BeanDefinitionRegistryPostProcessor类型的后置处理器名称列表
			for (String ppName : postProcessorNames) {
				//C03.05.01.07_1 如果后置处理器实现了PriorityOrdered接口，则将该后置处理器添加到当前正在处理的后置处理器集合中，添加到已处理的bwan集合中
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			// C03.05.01.08 排序不重要，况且currentRegistryProcessors这里也只有一个数据
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			// C03.05.01.09 合并list，不重要(为什么要合并，因为还有自己的)
			registryProcessors.addAll(currentRegistryProcessors);
			// 最重要。注意这里是方法调用
			// C03.05.01.10 执行所有BeanDefinitionRegistryPostProcessor的postProcessBeanDefinitionRegistry方法
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			// C03.05.01.11 执行完成了所有BeanDefinitionRegistryPostProcessor，清除当前正在处理的后置处理器集合
			//这个list只是一个临时变量，故而要清除
			currentRegistryProcessors.clear();

			// C03.05.01.12 接下来，调用实现Ordered的BeanDefinitionRegistryPostProcessor。
			//
			// 每次调用getBeanNamesForType，都会进行一次合并，因为每次执行PostProcessor都有可能添加新的bd，
			// 而新的bd还是需要进行合并然后再比较，尤其是ConfigurationPostProcessor中，会扫描现有所有的bd
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			currentRegistryProcessors.clear();

			// C03.05.01.13 最后，循环调用所有其他BeanDefinitionRegistryPostProcessor，直到不再出现其他bean为止。为什么要循环，是为了
			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					// 排除掉已经执行的后置处理器
					if (!processedBeans.contains(ppName)) {
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				registryProcessors.addAll(currentRegistryProcessors);
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
				currentRegistryProcessors.clear();
			}

			// 现在，调用到目前为止处理的所有处理器的postProcessBeanFactory回调。
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			// 调用用上下文实例注册的工厂处理器。
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		// 不要在这里初始化FactoryBean：我们需要不初始化所有常规bean，让bean工厂的后处理程序应用于它们!
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// 将实现优先排序、有序和其他功能的BeanFactoryPostProcessor分开。
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (processedBeans.contains(ppName)) {
				// 跳过-已经在上面的第一阶段处理
			}
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// 首先，调用实现PriorityOrdered的BeanFactoryPostProcessor。
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// 接下来，调用实现Ordered的BeanFactoryPostProcessor。
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// 最后，调用所有其他BeanFactoryPostProcessor.
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// 清除缓存的合并bean定义，因为后处理程序可能修改了原始元数据，例如替换值中的占位符……
		beanFactory.clearMetadataCache();
	}

	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {
		// 从beanDefinitionMap中得到所有的BeanPostProcessor
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// 注册BeanPostProcessorChecker，当bean在BeanPostProcessor实例化过程中被创建时，即当一个bean没有资格被所有BeanPostProcessor处理时，它记录一条信息消息。
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// 在实现优先排序、有序的beanpostprocessor和其他处理器之间进行分离。
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				// 判断是否实现了有序的优先级配置，添加到优先级处理器列表
				priorityOrderedPostProcessors.add(pp);
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				// 如果是继承了有序接口，则添加到有序处理器列表
				orderedPostProcessorNames.add(ppName);
			}
			else {
				// 否则添加到无序处理器列表
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// 1. 首先，注册实现了PriorityOrdered接口的BeanPostProcessors。
		// 先进行排序
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		// 然后进行注册
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// 2. 注册实现了Ordered接口的BeanPostProcessors。
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String ppName : orderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		// 3. 注册所有常规的BeanPostProcessors.
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// 4. 重新注册所有的内部BeanPostProcessors.
		sortPostProcessors(internalPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// 将检测内部bean的后处理器重新注册为ApplicationListeners，将其移动到处理器链的末尾(用于获取代理等)。
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	/**
	 * 使用继承的排序方式，排序已有的后置处理器
	 * @param postProcessors
	 * @param beanFactory
	 */
	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			comparatorToUse = OrderComparator.INSTANCE;
		}
		postProcessors.sort(comparatorToUse);
	}

	/**
	 * 调用给定的BeanDefinitionRegistryPostProcessor beans.
	 * 注意对比下面这个方法
	 * BeanDefinitionRegistryPostProcessor和BeanFactoryPostProcessor
	 *
	 * 处理并调用ConfigurationClassPostProcessor后置处理器的postProcessBeanDefinitionRegistry的方法
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry) {

		/**
		 * 遍历调用Bean定义注册后置处理器的相应逻辑
		 */
		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanDefinitionRegistry(registry);
		}
	}

	/**
	 * 请求给定的BeanFactoryPostProcessor bean.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanFactory(beanFactory);
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		for (BeanPostProcessor postProcessor : postProcessors) {
			beanFactory.addBeanPostProcessor(postProcessor);
		}
	}


	/**
	 * BeanPostProcessor，当bean在BeanPostProcessor实例化过程中被创建时，
	 * 即当一个bean没有资格被所有BeanPostProcessor处理时，它记录一条信息消息。
	 *
	 * <p>当Spring的配置中的后处理器还没有被注册就已经开始了bean的初始化
	 * 便会打印出BeanPostProcessorChecker中设定的信息
	 */
	private static final class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			// 非Spring自己的bean注册时，如果后处理器还没有注册完成，就开始处理，则记录消息，info级别
			if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}

		private boolean isInfrastructureBean(@Nullable String beanName) {
			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
				return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
			}
			return false;
		}
	}

}
