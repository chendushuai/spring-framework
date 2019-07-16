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

package org.springframework.core.io;

import org.springframework.lang.Nullable;
import org.springframework.util.ResourceUtils;

/**
 * 加载资源的策略接口(例如：类路径或文件系统资源)。
 * {@link org.springframework.context.ApplicationContext}必须提供此功能，
 * 还需要扩展的{@link org.springframework.core.io.support.ResourcePatternResolver}支持。
 *
 * <p>{@link DefaultResourceLoader}是一个独立的实现，可以在ApplicationContext之外使用，
 * 也可以由{@link ResourceEditor}使用。
 *
 * <p>当在ApplicationContext中运行时，
 * 可以使用特定上下文的资源加载策略从字符串填充Resource类型和Resource数组的Bean属性。
 *
 * @author Juergen Hoeller
 * @since 10.03.2004
 * @see Resource
 * @see org.springframework.core.io.support.ResourcePatternResolver
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.ResourceLoaderAware
 */
public interface ResourceLoader {

	/** 用于从类路径加载的伪URL前缀: "classpath:". */
	String CLASSPATH_URL_PREFIX = ResourceUtils.CLASSPATH_URL_PREFIX;


	/**
	 * 返回指定资源位置的资源句柄。
	 * <p>句柄应该始终是一个可重用的资源描述符，允许多个{@link Resource#getInputStream()}调用。
	 * <p><ul>
	 * <li>必须支持完全限定的url，例如： "file:C:/test.dat".
	 * <li>必须支持类路径伪url, e.g. "classpath:test.dat".
	 * <li>必须支持相对的文件路径, e.g. "WEB-INF/test.dat".
	 * (这将是特定于实现的，通常由ApplicationContext实现提供。)
	 * </ul>
	 * <p>注意，资源句柄并不意味着现有资源;还需要调用{@link Resource#exists}来检查资源是否存在。
	 * @param location 资源地址
	 * @return 对应的资源句柄(永远不会为{@code null})
	 * @see #CLASSPATH_URL_PREFIX
	 * @see Resource#exists()
	 * @see Resource#getInputStream()
	 */
	Resource getResource(String location);

	/**
	 * 公开这个ResourceLoader使用的类加载器。
	 * <p>需要直接访问类加载器ClassLoader的客户机可以使用ResourceLoader以统一的方式访问类加载器，
	 * 而不是依赖于线程上下文类加载器ClassLoader。
	 * @return 类加载器ClassLoader
	 * (只有在系统类加载器ClassLoader不是可访问的时候，才会返回{@code null})
	 * @see org.springframework.util.ClassUtils#getDefaultClassLoader()
	 * @see org.springframework.util.ClassUtils#forName(String, ClassLoader)
	 */
	@Nullable
	ClassLoader getClassLoader();

}
