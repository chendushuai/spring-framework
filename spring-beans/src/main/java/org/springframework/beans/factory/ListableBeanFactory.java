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

package org.springframework.beans.factory;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * Extension of the {@link BeanFactory} interface to be implemented by bean factories
 * that can enumerate all their bean instances, rather than attempting bean lookup
 * by name one by one as requested by clients. BeanFactory implementations that
 * preload all their bean definitions (such as XML-based factories) may implement
 * this interface.
 *
 * <p>If this is a {@link HierarchicalBeanFactory}, the return values will <i>not</i>
 * take any BeanFactory hierarchy into account, but will relate only to the beans
 * defined in the current factory. Use the {@link BeanFactoryUtils} helper class
 * to consider beans in ancestor factories too.
 *
 * <p>The methods in this interface will just respect bean definitions of this factory.
 * They will ignore any singleton beans that have been registered by other means like
 * {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}'s
 * {@code registerSingleton} method, with the exception of
 * {@code getBeanNamesOfType} and {@code getBeansOfType} which will check
 * such manually registered singletons too. Of course, BeanFactory's {@code getBean}
 * does allow transparent access to such special beans as well. However, in typical
 * scenarios, all beans will be defined by external bean definitions anyway, so most
 * applications don't need to worry about this differentiation.
 *
 * <p><b>NOTE:</b> With the exception of {@code getBeanDefinitionCount}
 * and {@code containsBeanDefinition}, the methods in this interface
 * are not designed for frequent invocation. Implementations may be slow.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 16 April 2001
 * @see HierarchicalBeanFactory
 * @see BeanFactoryUtils
 */
public interface ListableBeanFactory extends BeanFactory {

	/**
	 * Check if this bean factory contains a bean definition with the given name.
	 * <p>Does not consider any hierarchy this factory may participate in,
	 * and ignores any singleton beans that have been registered by
	 * other means than bean definitions.
	 * @param beanName the name of the bean to look for
	 * @return if this bean factory contains a bean definition with the given name
	 * @see #containsBean
	 */
	boolean containsBeanDefinition(String beanName);

	/**
	 * Return the number of beans defined in the factory.
	 * <p>Does not consider any hierarchy this factory may participate in,
	 * and ignores any singleton beans that have been registered by
	 * other means than bean definitions.
	 * @return the number of beans defined in the factory
	 */
	int getBeanDefinitionCount();

	/**
	 * Return the names of all beans defined in this factory.
	 * <p>Does not consider any hierarchy this factory may participate in,
	 * and ignores any singleton beans that have been registered by
	 * other means than bean definitions.
	 * @return the names of all beans defined in this factory,
	 * or an empty array if none defined
	 */
	String[] getBeanDefinitionNames();

	/**
	 * 根据bean定义或FactoryBean中{@code getObjectType}的值判断，返回与给定类型(包括子类)匹配的bean的名称。
	 * <p><b>注意:此方法仅内省顶级bean。</b> 它也<i>不检查</i>可能匹配指定类型的嵌套bean。
	 * <p>确实考虑了FactoryBean创建的对象，这意味着FactoryBean将被初始化。
	 * 如果FactoryBean创建的对象不匹配，原始的FactoryBean本身将根据类型进行匹配。
	 * <p>不考虑本工厂可能参与的任何层级。
	 * 使用BeanFactoryUtils的{@code beanNamesForTypeIncludingAncestors}在祖先工厂中包含bean。
	 * <p>注意:<i>不要</i>忽略通过bean定义以外的其他方式注册的单例bean。
	 * <p>这个版本的 {@code getBeanNamesForType}匹配所有类型的bean，无论是单例bean、原型bean还是FactoryBean。
	 * 在大多数实现中，结果与{@code getBeanNamesForType(type, true, true)}相同。
	 * <p>此方法返回的Bean名称应尽可能<i>按后端配置中定义的顺序</i>返回。
	 * @param type 要匹配的泛型类型的类或接口
	 * @return 与给定对象类型(包括子类)匹配的bean(或FactoryBeans创建的对象)的名称，如果没有，则为空数组
	 * @since 4.2
	 * @see #isTypeMatch(String, ResolvableType)
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors(ListableBeanFactory, ResolvableType)
	 */
	String[] getBeanNamesForType(ResolvableType type);

	/**
	 * Return the names of beans matching the given type (including subclasses),
	 * judging from either bean definitions or the value of {@code getObjectType}
	 * in the case of FactoryBeans.
	 * <p><b>NOTE: This method introspects top-level beans only.</b> It does <i>not</i>
	 * check nested beans which might match the specified type as well.
	 * <p>Does consider objects created by FactoryBeans, which means that FactoryBeans
	 * will get initialized. If the object created by the FactoryBean doesn't match,
	 * the raw FactoryBean itself will be matched against the type.
	 * <p>Does not consider any hierarchy this factory may participate in.
	 * Use BeanFactoryUtils' {@code beanNamesForTypeIncludingAncestors}
	 * to include beans in ancestor factories too.
	 * <p>Note: Does <i>not</i> ignore singleton beans that have been registered
	 * by other means than bean definitions.
	 * <p>This version of {@code getBeanNamesForType} matches all kinds of beans,
	 * be it singletons, prototypes, or FactoryBeans. In most implementations, the
	 * result will be the same as for {@code getBeanNamesForType(type, true, true)}.
	 * <p>Bean names returned by this method should always return bean names <i>in the
	 * order of definition</i> in the backend configuration, as far as possible.
	 * @param type the class or interface to match, or {@code null} for all bean names
	 * @return the names of beans (or objects created by FactoryBeans) matching
	 * the given object type (including subclasses), or an empty array if none
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors(ListableBeanFactory, Class)
	 */
	String[] getBeanNamesForType(@Nullable Class<?> type);

	/**
	 * 返回与给定类型(包括子类)匹配的bean的名称，根据bean定义或FactoryBeans中{@code getObjectType}的值判断。
	 *
	 * <p><b>注意:此方法仅内省顶级bean。</b> 它<i>也不检查</i>可能匹配指定类型的嵌套bean。
	 *
	 * <p>如果设置了“allowEagerInit”标志，请考虑FactoryBean创建的对象，这意味着将初始化FactoryBean。
	 * 如果FactoryBean创建的对象不匹配，原始的FactoryBean本身将根据类型进行匹配。
	 * 如果没有设置“allowEagerInit”，那么只检查原始的FactoryBean(不需要初始化每个FactoryBean)。
	 *
	 * <p>不考虑此工厂可能参与的任何层次结构。使用BeanFactoryUtil的
	 * {@code beanNamesForTypeIncludingAncestors}也可以在祖先工厂中包含bean。
	 *
	 * <p>注意:<i>不要</i>忽略通过bean定义之外的其他方法注册的单例bean。
	 *
	 * <p>此方法返回的Bean名称应尽可能按照后端配置中的<i>定义顺序</i>返回Bean名称。
	 *
	 * @param type 要匹配的类或接口，或所有bean名称的{@code null}
	 * @param includeNonSingletons 是否也包含原型bean或作用域bean，或者只包含单例bean(也适用于工厂bean)
	 * @param allowEagerInit 是否初始化<i>懒加载单例</i>和用于类型检查的<i>由FactoryBeans创建的对象</i>(或由具有“factory-bean”引用的工厂方法)。
	 * 注意，必须提前地初始化FactoryBeans来确定它们的类型：所以要注意，为这个标志传递“true”将初始化FactoryBeans和“factory-bean”引用。
	 * @return 匹配给定对象类型(包括子类)的bean(或FactoryBeans创建的对象)的名称，如果没有，则为空数组
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors(ListableBeanFactory, Class, boolean, boolean)
	 */
	String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit);

	/**
	 * Return the bean instances that match the given object type (including
	 * subclasses), judging from either bean definitions or the value of
	 * {@code getObjectType} in the case of FactoryBeans.
	 * <p><b>NOTE: This method introspects top-level beans only.</b> It does <i>not</i>
	 * check nested beans which might match the specified type as well.
	 * <p>Does consider objects created by FactoryBeans, which means that FactoryBeans
	 * will get initialized. If the object created by the FactoryBean doesn't match,
	 * the raw FactoryBean itself will be matched against the type.
	 * <p>Does not consider any hierarchy this factory may participate in.
	 * Use BeanFactoryUtils' {@code beansOfTypeIncludingAncestors}
	 * to include beans in ancestor factories too.
	 * <p>Note: Does <i>not</i> ignore singleton beans that have been registered
	 * by other means than bean definitions.
	 * <p>This version of getBeansOfType matches all kinds of beans, be it
	 * singletons, prototypes, or FactoryBeans. In most implementations, the
	 * result will be the same as for {@code getBeansOfType(type, true, true)}.
	 * <p>The Map returned by this method should always return bean names and
	 * corresponding bean instances <i>in the order of definition</i> in the
	 * backend configuration, as far as possible.
	 * @param type the class or interface to match, or {@code null} for all concrete beans
	 * @return a Map with the matching beans, containing the bean names as
	 * keys and the corresponding bean instances as values
	 * @throws BeansException if a bean could not be created
	 * @since 1.1.2
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beansOfTypeIncludingAncestors(ListableBeanFactory, Class)
	 */
	<T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException;

	/**
	 * 根据bean定义或FactoryBeans中{@code getObjectType}的值判断，返回与给定对象类型(包括子类)匹配的bean实例。
	 * <p><b>注意:此方法仅内省顶级bean</b>。
	 * 它也<i>不检查</i>可能匹配指定类型的嵌套bean。
	 * <p>如果设置了“allowEagerInit”标志，那么可以考虑FactoryBeans创建的对象，这意味着FactoryBeans将被初始化。
	 * 如果FactoryBean创建的对象不匹配，原始的FactoryBean本身将根据类型进行匹配。
	 * 如果没有设置“allowEagerInit”，那么只检查原始的FactoryBean(不需要初始化每个FactoryBean)。
	 * <p>不考虑本工厂可能参与的任何层级。
	 * 使用BeanFactoryUtils的{@code beansOfTypeIncludingAncestors}在始祖工厂中包含bean。
	 * <p>注意:<i>不要</i>忽略通过bean定义以外的其他方式注册的单例bean。
	 * <p>此方法返回的映射应该总是尽可能按照后端配置中的<i>定义的顺序</i>返回bean名称和相应的bean实例。
	 * @param type 要匹配的类或接口，或所有具体bean的{@code null}
	 * @param includeNonSingletons 是否也包括原型bean或作用域bean，或者只是单例bean(也适用于FactoryBeans)
	 * @param allowEagerInit 是否为类型检查初始化<i>由FactoryBeans对象</i>和<i>延迟初始化的单例对象</i>
	 *                       (或由具有“factory-bean”引用的工厂方法)。
	 *                       注意，必须立即初始化FactoryBeans以确定它们的类型:
	 *                       因此要注意，为这个标志传递“true”将初始化FactoryBeans和“factory-bean”引用。
	 * @return 具有匹配bean的映射，其中包含作为键的bean名称和作为值的相应bean实例
	 * @throws BeansException 如果无法创建bean
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beansOfTypeIncludingAncestors(ListableBeanFactory, Class, boolean, boolean)
	 */
	<T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException;

	/**
	 * Find all names of beans which are annotated with the supplied {@link Annotation}
	 * type, without creating corresponding bean instances yet.
	 * <p>Note that this method considers objects created by FactoryBeans, which means
	 * that FactoryBeans will get initialized in order to determine their object type.
	 * @param annotationType the type of annotation to look for
	 * (at class, interface or factory method level of the specified bean)
	 * @return the names of all matching beans
	 * @since 4.0
	 * @see #findAnnotationOnBean
	 */
	String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType);

	/**
	 * Find all beans which are annotated with the supplied {@link Annotation} type,
	 * returning a Map of bean names with corresponding bean instances.
	 * <p>Note that this method considers objects created by FactoryBeans, which means
	 * that FactoryBeans will get initialized in order to determine their object type.
	 * @param annotationType the type of annotation to look for
	 * (at class, interface or factory method level of the specified bean)
	 * @return a Map with the matching beans, containing the bean names as
	 * keys and the corresponding bean instances as values
	 * @throws BeansException if a bean could not be created
	 * @since 3.0
	 * @see #findAnnotationOnBean
	 */
	Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) throws BeansException;

	/**
	 * Find an {@link Annotation} of {@code annotationType} on the specified bean,
	 * traversing its interfaces and super classes if no annotation can be found on
	 * the given class itself, as well as checking the bean's factory method (if any).
	 * @param beanName the name of the bean to look for annotations on
	 * @param annotationType the type of annotation to look for
	 * (at class, interface or factory method level of the specified bean)
	 * @return the annotation of the given type if found, or {@code null} otherwise
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given name
	 * @since 3.0
	 * @see #getBeanNamesForAnnotation
	 * @see #getBeansWithAnnotation
	 */
	@Nullable
	<A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
			throws NoSuchBeanDefinitionException;

}
