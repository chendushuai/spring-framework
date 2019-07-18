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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.SimpleAliasRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 共享bean实例的通用注册表，实现
 * {@link org.springframework.beans.factory.config.SingletonBeanRegistry}。
 * 允许注册单例实例，注册表的所有调用者都应该共享这些实例，这些实例可以通过bean名称获得。
 *
 * <p>还支持{@link org.springframework.beans.factory.DisposableBean}实例注册，
 * (它可能对应于已注册的单例，也可能不对应于已注册的单例)，在注册表关闭时会被销毁。
 * 可以注册bean之间的依赖关系来强制执行适当的关机命令。
 *
 * <p>这个类主要用作{@link org.springframework.beans.factory.BeanFactory}实现的基类，
 * 分解出对单例bean实例的公共管理。请注意{@link org.springframework.beans.factory.config.ConfigurableBeanFactory}
 * 接口扩展了{@link SingletonBeanRegistry}接口。
 *
 * <p>注意，与{@link AbstractBeanFactory}和{@link DefaultListableBeanFactory}
 * (继承自它)相比，该类既不假定bean定义概念，也不假定bean实例的特定创建过程。
 * 也可以用作委托给的嵌套助手。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #registerSingleton
 * @see #registerDisposableBean
 * @see org.springframework.beans.factory.DisposableBean
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory
 */
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {

	/**
	 * 单例对象的缓存：bean名称到bean实现
	 * <p>用于存放完全初始化好的 bean，从该缓存中取出的 bean可以直接使用
	 */
	private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

	/**
	 * 单例工厂的缓存：bean名称到对象工厂
	 * 存放 bean工厂对象解决循环依赖
	 */
	private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);

	/**
	 * 早期单例对象的缓存：bean名称到bean实例。
	 * 存放原始的bean对象用于解决循环依赖,注意：存到里面的对象还没有被填充属性
	 */
	private final Map<String, Object> earlySingletonObjects = new HashMap<>(16);

	/** 一组已注册的单例程序，按注册顺序包含bean名称。 */
	private final Set<String> registeredSingletons = new LinkedHashSet<>(256);

	/** 当前正在创建的bean的名称。 */
	private final Set<String> singletonsCurrentlyInCreation =
			Collections.newSetFromMap(new ConcurrentHashMap<>(16));

	/** 当前在创建检查中排除的bean名称。 */
	private final Set<String> inCreationCheckExclusions =
			Collections.newSetFromMap(new ConcurrentHashMap<>(16));

	/** 被抑制的异常列表，用于关联相关原因。 */
	@Nullable
	private Set<Exception> suppressedExceptions;

	/** 标志，指示我们当前是否在destroySingletons中。 */
	private boolean singletonsCurrentlyInDestruction = false;

	/** 一次性bean实例：bean名称到一次性实例。*/
	private final Map<String, Object> disposableBeans = new LinkedHashMap<>();

	/** 包含bean名称之间的映射：bean名称到bean包含的bean名称集合。 */
	private final Map<String, Set<String>> containedBeanMap = new ConcurrentHashMap<>(16);

	/** 从属bean名称之间的映射：从bean名称映射到从属bean名称集合。 */
	private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<>(64);

	/** 在依赖bean名称之间映射：bean名称到bean依赖项的bean名称集合。 */
	private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<>(64);


	@Override
	public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
		Assert.notNull(beanName, "Bean name must not be null");
		Assert.notNull(singletonObject, "Singleton object must not be null");
		synchronized (this.singletonObjects) {
			Object oldObject = this.singletonObjects.get(beanName);
			if (oldObject != null) {
				throw new IllegalStateException("Could not register object [" + singletonObject +
						"] under bean name '" + beanName + "': there is already object [" + oldObject + "] bound");
			}
			addSingleton(beanName, singletonObject);
		}
	}

	/**
	 * 将给定的单例对象添加到该工厂的单例缓存中。
	 * <p>要求尽快注册单例对象
	 * @param beanName bean名称
	 * @param singletonObject 单例对象
	 */
	protected void addSingleton(String beanName, Object singletonObject) {
		synchronized (this.singletonObjects) {
			this.singletonObjects.put(beanName, singletonObject);
			this.singletonFactories.remove(beanName);
			this.earlySingletonObjects.remove(beanName);
			this.registeredSingletons.add(beanName);
		}
	}

	/**
	 * 如果需要，添加用于构建指定单例的给定单例工厂。
	 * <p>要求快速注册单例，例如能够解析循环引用。
	 * @param beanName bean名称
	 * @param singletonFactory 单例对象的工厂
	 */
	protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(singletonFactory, "Singleton factory must not be null");
		synchronized (this.singletonObjects) {
			if (!this.singletonObjects.containsKey(beanName)) {
				this.singletonFactories.put(beanName, singletonFactory);
				this.earlySingletonObjects.remove(beanName);
				this.registeredSingletons.add(beanName);
			}
		}
	}

	@Override
	@Nullable
	public Object getSingleton(String beanName) {
		return getSingleton(beanName, true);
	}

	/**
	 * 返回在给定名称下注册的(原始)单例对象。
	 * <p>检查已实例化的单例，并允许对当前创建的单例的早期引用(解析循环引用)。
	 * @param beanName 要查找的bean的名称
	 * @param allowEarlyReference 是否应该创建早期引用
	 * @return 注册的单例对象，如果没有找到，则为{@code null}
	 */
	@Nullable
	protected Object getSingleton(String beanName, boolean allowEarlyReference) {
		// 从已创建的单例对象找到对应名称的bean
		Object singletonObject = this.singletonObjects.get(beanName);
		// 如果当前bean为未创建，并且是当前正在创建的单例对象
		if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
			synchronized (this.singletonObjects) {
				// 获取该名称对应的早期单例对象缓存，不包含属性
				singletonObject = this.earlySingletonObjects.get(beanName);
				// 如果早期单例对象缓存为空，且允许创建早期引用对象
				if (singletonObject == null && allowEarlyReference) {
					// 从单例工厂中获取创建对应Bean
					ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
					if (singletonFactory != null) {
						singletonObject = singletonFactory.getObject();
						// 保存创建的早期引用对象
						this.earlySingletonObjects.put(beanName, singletonObject);
						// 移除单例工厂中的该对象
						this.singletonFactories.remove(beanName);
					}
				}
			}
		}
		return singletonObject;
	}

	/**
	 * 返回在给定名称下注册的(原始)单例对象，如果还没有注册，则创建并注册一个新的单例对象。
	 * @param beanName bean名称
	 * @param singletonFactory 如果需要，ObjectFactory可以延迟创建单例
	 * @return 注册的单例对象
	 */
	public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(beanName, "Bean name must not be null");
		synchronized (this.singletonObjects) {
			// 获取已经创建的单例bean对象
			Object singletonObject = this.singletonObjects.get(beanName);
			if (singletonObject == null) {
				if (this.singletonsCurrentlyInDestruction) {
					// 当这个工厂的单例对象被销毁时，不允许创建单例bean(不要在销毁方法实现中向BeanFactory请求bean!)
					throw new BeanCreationNotAllowedException(beanName,
							"Singleton bean creation not allowed while singletons of this factory are in destruction " +
							"(Do not request a bean from a BeanFactory in a destroy method implementation!)");
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
				}
				/**
				 * 将beanName添加到singletonsCurrentlyInCreation这样一个set集合中
				 * 表示beanName对应的bean正在创建中
				 */
				beforeSingletonCreation(beanName);
				boolean newSingleton = false;
				boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
				if (recordSuppressedExceptions) {
					this.suppressedExceptions = new LinkedHashSet<>();
				}
				try {
					singletonObject = singletonFactory.getObject();
					newSingleton = true;
				}
				catch (IllegalStateException ex) {
					// Has the singleton object implicitly appeared in the meantime ->
					// if yes, proceed with it since the exception indicates that state.
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {
						throw ex;
					}
				}
				catch (BeanCreationException ex) {
					if (recordSuppressedExceptions) {
						for (Exception suppressedException : this.suppressedExceptions) {
							ex.addRelatedCause(suppressedException);
						}
					}
					throw ex;
				}
				finally {
					if (recordSuppressedExceptions) {
						this.suppressedExceptions = null;
					}
					afterSingletonCreation(beanName);
				}
				if (newSingleton) {
					addSingleton(beanName, singletonObject);
				}
			}
			return singletonObject;
		}
	}

	/**
	 * 注册在创建单例bean实例时被抑制的异常，例如临时循环引用解析问题。
	 * @param ex 要注册的异常
	 */
	protected void onSuppressedException(Exception ex) {
		synchronized (this.singletonObjects) {
			if (this.suppressedExceptions != null) {
				this.suppressedExceptions.add(ex);
			}
		}
	}

	/**
	 * 从该工厂的单例缓存中删除具有给定名称的bean，以便在创建失败时清理单例的提前注册。
	 * @param beanName bean的名称
	 * @see #getSingletonMutex()
	 */
	protected void removeSingleton(String beanName) {
		synchronized (this.singletonObjects) {
			this.singletonObjects.remove(beanName);
			this.singletonFactories.remove(beanName);
			this.earlySingletonObjects.remove(beanName);
			this.registeredSingletons.remove(beanName);
		}
	}

	@Override
	public boolean containsSingleton(String beanName) {
		return this.singletonObjects.containsKey(beanName);
	}

	@Override
	public String[] getSingletonNames() {
		synchronized (this.singletonObjects) {
			return StringUtils.toStringArray(this.registeredSingletons);
		}
	}

	@Override
	public int getSingletonCount() {
		synchronized (this.singletonObjects) {
			return this.registeredSingletons.size();
		}
	}


	public void setCurrentlyInCreation(String beanName, boolean inCreation) {
		Assert.notNull(beanName, "Bean name must not be null");
		if (!inCreation) {
			this.inCreationCheckExclusions.add(beanName);
		}
		else {
			this.inCreationCheckExclusions.remove(beanName);
		}
	}

	public boolean isCurrentlyInCreation(String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		return (!this.inCreationCheckExclusions.contains(beanName) && isActuallyInCreation(beanName));
	}

	protected boolean isActuallyInCreation(String beanName) {
		return isSingletonCurrentlyInCreation(beanName);
	}

	/**
	 * 返回指定的单例bean当前是否正在创建(在整个工厂内)。
	 * @param beanName bean的名称
	 */
	public boolean isSingletonCurrentlyInCreation(String beanName) {
		return this.singletonsCurrentlyInCreation.contains(beanName);
	}

	/**
	 * 在单例对象创建前回调
	 * <p>默认实现将单例注册为当前正在创建的单例。
	 * @param beanName 将要创建的单例的名称
	 * @see #isSingletonCurrentlyInCreation
	 */
	protected void beforeSingletonCreation(String beanName) {
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.add(beanName)) {
			throw new BeanCurrentlyInCreationException(beanName);
		}
	}

	/**
	 * Callback after singleton creation.
	 * <p>The default implementation marks the singleton as not in creation anymore.
	 * @param beanName the name of the singleton that has been created
	 * @see #isSingletonCurrentlyInCreation
	 */
	protected void afterSingletonCreation(String beanName) {
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.remove(beanName)) {
			throw new IllegalStateException("Singleton '" + beanName + "' isn't currently in creation");
		}
	}


	/**
	 * Add the given bean to the list of disposable beans in this registry.
	 * <p>Disposable beans usually correspond to registered singletons,
	 * matching the bean name but potentially being a different instance
	 * (for example, a DisposableBean adapter for a singleton that does not
	 * naturally implement Spring's DisposableBean interface).
	 * @param beanName the name of the bean
	 * @param bean the bean instance
	 */
	public void registerDisposableBean(String beanName, DisposableBean bean) {
		synchronized (this.disposableBeans) {
			this.disposableBeans.put(beanName, bean);
		}
	}

	/**
	 * Register a containment relationship between two beans,
	 * e.g. between an inner bean and its containing outer bean.
	 * <p>Also registers the containing bean as dependent on the contained bean
	 * in terms of destruction order.
	 * @param containedBeanName the name of the contained (inner) bean
	 * @param containingBeanName the name of the containing (outer) bean
	 * @see #registerDependentBean
	 */
	public void registerContainedBean(String containedBeanName, String containingBeanName) {
		synchronized (this.containedBeanMap) {
			Set<String> containedBeans =
					this.containedBeanMap.computeIfAbsent(containingBeanName, k -> new LinkedHashSet<>(8));
			if (!containedBeans.add(containedBeanName)) {
				return;
			}
		}
		registerDependentBean(containedBeanName, containingBeanName);
	}

	/**
	 * 为给定bean注册一个依赖bean，在销毁给定bean之前销毁它。
	 * @param beanName bean名称
	 * @param dependentBeanName 依赖bean名称
	 */
	public void registerDependentBean(String beanName, String dependentBeanName) {
		// 解析别名
		String canonicalName = canonicalName(beanName);

		synchronized (this.dependentBeanMap) {
			Set<String> dependentBeans =
					this.dependentBeanMap.computeIfAbsent(canonicalName, k -> new LinkedHashSet<>(8));
			if (!dependentBeans.add(dependentBeanName)) {
				return;
			}
		}

		synchronized (this.dependenciesForBeanMap) {
			Set<String> dependenciesForBean =
					this.dependenciesForBeanMap.computeIfAbsent(dependentBeanName, k -> new LinkedHashSet<>(8));
			dependenciesForBean.add(canonicalName);
		}
	}

	/**
	 * 确定指定的依赖bean是否已注册为依赖于给定bean或依赖于其传递依赖项。
	 * @param beanName 要检查的bean名称
	 * @param dependentBeanName 依赖bean名称
	 * @since 4.0
	 */
	protected boolean isDependent(String beanName, String dependentBeanName) {
		synchronized (this.dependentBeanMap) {
			return isDependent(beanName, dependentBeanName, null);
		}
	}

	/**
	 * 判断是否存在依赖关系
	 * @param beanName 指定bean名称
	 * @param dependentBeanName 依赖bean名称
	 * @param alreadySeen
	 * @return
	 */
	private boolean isDependent(String beanName, String dependentBeanName, @Nullable Set<String> alreadySeen) {
		if (alreadySeen != null && alreadySeen.contains(beanName)) {
			return false;
		}
		// 解析别名为规范名称
		String canonicalName = canonicalName(beanName);
		// 查询给定bean的依赖关系
		Set<String> dependentBeans = this.dependentBeanMap.get(canonicalName);
		if (dependentBeans == null) {
			return false;
		}
		if (dependentBeans.contains(dependentBeanName)) {
			return true;
		}
		// 解析依赖的依赖，并未直接的依赖关系
		for (String transitiveDependency : dependentBeans) {
			if (alreadySeen == null) {
				alreadySeen = new HashSet<>();
			}
			alreadySeen.add(beanName);
			if (isDependent(transitiveDependency, dependentBeanName, alreadySeen)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine whether a dependent bean has been registered for the given name.
	 * @param beanName the name of the bean to check
	 */
	protected boolean hasDependentBean(String beanName) {
		return this.dependentBeanMap.containsKey(beanName);
	}

	/**
	 * Return the names of all beans which depend on the specified bean, if any.
	 * @param beanName the name of the bean
	 * @return the array of dependent bean names, or an empty array if none
	 */
	public String[] getDependentBeans(String beanName) {
		Set<String> dependentBeans = this.dependentBeanMap.get(beanName);
		if (dependentBeans == null) {
			return new String[0];
		}
		synchronized (this.dependentBeanMap) {
			return StringUtils.toStringArray(dependentBeans);
		}
	}

	/**
	 * Return the names of all beans that the specified bean depends on, if any.
	 * @param beanName the name of the bean
	 * @return the array of names of beans which the bean depends on,
	 * or an empty array if none
	 */
	public String[] getDependenciesForBean(String beanName) {
		Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(beanName);
		if (dependenciesForBean == null) {
			return new String[0];
		}
		synchronized (this.dependenciesForBeanMap) {
			return StringUtils.toStringArray(dependenciesForBean);
		}
	}

	public void destroySingletons() {
		if (logger.isTraceEnabled()) {
			logger.trace("Destroying singletons in " + this);
		}
		synchronized (this.singletonObjects) {
			this.singletonsCurrentlyInDestruction = true;
		}

		String[] disposableBeanNames;
		synchronized (this.disposableBeans) {
			disposableBeanNames = StringUtils.toStringArray(this.disposableBeans.keySet());
		}
		for (int i = disposableBeanNames.length - 1; i >= 0; i--) {
			destroySingleton(disposableBeanNames[i]);
		}

		this.containedBeanMap.clear();
		this.dependentBeanMap.clear();
		this.dependenciesForBeanMap.clear();

		clearSingletonCache();
	}

	/**
	 * Clear all cached singleton instances in this registry.
	 * @since 4.3.15
	 */
	protected void clearSingletonCache() {
		synchronized (this.singletonObjects) {
			this.singletonObjects.clear();
			this.singletonFactories.clear();
			this.earlySingletonObjects.clear();
			this.registeredSingletons.clear();
			this.singletonsCurrentlyInDestruction = false;
		}
	}

	/**
	 * 销毁给定的bean。如果找到相应的一次性bean实例，则委托给{@code destroyBean}。
	 * @param beanName bean的名称
	 * @see #destroyBean
	 */
	public void destroySingleton(String beanName) {
		// 删除已注册的给定名称的单例(如果有的话)。
		removeSingleton(beanName);

		// 销毁当前DisposableBean实例
		DisposableBean disposableBean;
		synchronized (this.disposableBeans) {
			disposableBean = (DisposableBean) this.disposableBeans.remove(beanName);
		}
		// 销毁给定的bean
		destroyBean(beanName, disposableBean);
	}

	/**
	 * 销毁给定的bean。必须在bean本身之前销毁依赖于给定bean的bean。不应该抛出任何异常。
	 * @param beanName bean的名称
	 * @param bean 要销毁的bean实例
	 */
	protected void destroyBean(String beanName, @Nullable DisposableBean bean) {
		// 首先触发依赖bean的销毁……
		// 此处销毁的是依赖的对应关系
		Set<String> dependencies;
		synchronized (this.dependentBeanMap) {
			// 在完全同步的情况下，以保证一个断开连接的集
			dependencies = this.dependentBeanMap.remove(beanName);
		}
		// 如果存在依赖bean集合
		if (dependencies != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Retrieved dependent beans for bean '" + beanName + "': " + dependencies);
			}
			// 遍历逐个销毁依赖bean
			for (String dependentBeanName : dependencies) {
				destroySingleton(dependentBeanName);
			}
		}

		// 现在就销毁bean……
		if (bean != null) {
			try {
				bean.destroy();
			}
			catch (Throwable ex) {
				if (logger.isInfoEnabled()) {
					logger.info("Destroy method on bean with name '" + beanName + "' threw an exception", ex);
				}
			}
		}

		// 触发包含的bean的销毁……
		Set<String> containedBeans;
		synchronized (this.containedBeanMap) {
			// 在完全同步的情况下，以保证一个断开连接的集
			containedBeans = this.containedBeanMap.remove(beanName);
		}
		// 如果存在从属bean集合
		if (containedBeans != null) {
			for (String containedBeanName : containedBeans) {
				destroySingleton(containedBeanName);
			}
		}

		// 从其他bean的依赖项中删除已销毁的bean。
		synchronized (this.dependentBeanMap) {
			for (Iterator<Map.Entry<String, Set<String>>> it = this.dependentBeanMap.entrySet().iterator(); it.hasNext();) {
				Map.Entry<String, Set<String>> entry = it.next();
				Set<String> dependenciesToClean = entry.getValue();
				dependenciesToClean.remove(beanName);
				if (dependenciesToClean.isEmpty()) {
					it.remove();
				}
			}
		}

		// 删除已销毁的bean的准备依赖项信息。防止被再次依赖
		this.dependenciesForBeanMap.remove(beanName);
	}

	/**
	 * Exposes the singleton mutex to subclasses and external collaborators.
	 * <p>Subclasses should synchronize on the given Object if they perform
	 * any sort of extended singleton creation phase. In particular, subclasses
	 * should <i>not</i> have their own mutexes involved in singleton creation,
	 * to avoid the potential for deadlocks in lazy-init situations.
	 */
	public final Object getSingletonMutex() {
		return this.singletonObjects;
	}

}
