/*
 * Copyright 2002-2017 the original author or authors.
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ResourceLoader}接口的默认实现。由{@link ResourceEditor}使用，并作为
 * {@link org.springframework.context.support.AbstractApplicationContext}
 * 的基类。也可以单独使用。
 *
 * <p>如果位置值是URL，则返回{@link UrlResource}；如果非URL路径或“classpath:”伪URL，则返回{@link ClassPathResource}。
 *
 * @author Juergen Hoeller
 * @since 10.03.2004
 * @see FileSystemResourceLoader
 * @see org.springframework.context.support.ClassPathXmlApplicationContext
 */
public class DefaultResourceLoader implements ResourceLoader {

	/**
	 * 类加载器
	 */
	@Nullable
	private ClassLoader classLoader;

	/**
	 * 协议解析器集合
	 */
	private final Set<ProtocolResolver> protocolResolvers = new LinkedHashSet<>(4);

	private final Map<Class<?>, Map<Resource, ?>> resourceCaches = new ConcurrentHashMap<>(4);


	/**
	 * C01.01 创建一个新的DefaultResourceLoader.
	 * <p>类加载器访问将在这个ResourceLoader初始化时使用线程上下文类加载器进行。
	 * @see java.lang.Thread#getContextClassLoader()
	 */
	public DefaultResourceLoader() {
		this.classLoader = ClassUtils.getDefaultClassLoader();
	}

	/**
	 * 创建一个新的DefaultResourceLoader.
	 * @param classLoader 类加载器，用于加载类路径资源，或在实际访问资源时使用线程上下文类加载器时，参数为{@code null}
	 */
	public DefaultResourceLoader(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	/**
	 * 指定要加载类路径资源的类加载器，如果在实际访问资源时使用线程上下文类加载器，则参数为{@code null}。
	 * <p>默认情况下，类加载器访问将在这个ResourceLoader初始化时使用线程上下文类加载器进行。
	 */
	public void setClassLoader(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * 返回类加载器以加载类路径资源。
	 * <p>将传递给ClassPathResource的构造函数，该构造函数用于此资源加载器创建的所有ClassPathResource对象。
	 * @see ClassPathResource
	 */
	@Override
	@Nullable
	public ClassLoader getClassLoader() {
		return (this.classLoader != null ? this.classLoader : ClassUtils.getDefaultClassLoader());
	}

	/**
	 * 将给定的解析器注册到此资源加载器，允许处理其他协议。
	 * <p>任何此类解析器都将在此加载程序的标准解析规则之前调用。因此，它也可能覆盖任何默认规则。
	 * @since 4.3
	 * @see #getProtocolResolvers()
	 */
	public void addProtocolResolver(ProtocolResolver resolver) {
		Assert.notNull(resolver, "ProtocolResolver must not be null");
		this.protocolResolvers.add(resolver);
	}

	/**
	 * 返回当前注册的协议解析器的集合，允许自省和修改。
	 * @since 4.3
	 */
	public Collection<ProtocolResolver> getProtocolResolvers() {
		return this.protocolResolvers;
	}

	/**
	 * 获取给定值类型的缓存，由{@link Resource}键控。
	 * @param valueType 值类型, 例如，一个ASM {@code MetadataReader}
	 * @return 缓存{@link Map}，在{@code ResourceLoader}级别共享
	 * @since 5.0
	 */
	@SuppressWarnings("unchecked")
	public <T> Map<Resource, T> getResourceCache(Class<T> valueType) {
		return (Map<Resource, T>) this.resourceCaches.computeIfAbsent(valueType, key -> new ConcurrentHashMap<>());
	}

	/**
	 * 在此资源加载器中清空所有资源缓存
	 * @since 5.0
	 * @see #getResourceCache
	 */
	public void clearResourceCaches() {
		this.resourceCaches.clear();
	}


	/**
	 * 根据指定路径加载资源
	 * @param location 资源地址
	 * @return
	 */
	@Override
	public Resource getResource(String location) {
		Assert.notNull(location, "Location must not be null");

		// 使用给定的资源解析器集合解析资源
		for (ProtocolResolver protocolResolver : this.protocolResolvers) {
			Resource resource = protocolResolver.resolve(location, this);
			if (resource != null) {
				// 如果解析成功，直接返回
				return resource;
			}
		}

		// 如果使用斜杠开头，默认为应用程序根目录
		if (location.startsWith("/")) {
			return getResourceByPath(location);
		}
		// 如果给定路径使用类路径默认开头，如classpath:
		else if (location.startsWith(CLASSPATH_URL_PREFIX)) {
			// 则截取路径后，使用类加载器获取文件路径
			return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()), getClassLoader());
		}
		else {
			try {
				// 尝试将位置转换为URL...
				URL url = new URL(location);
				return (ResourceUtils.isFileURL(url) ? new FileUrlResource(url) : new UrlResource(url));
			}
			catch (MalformedURLException ex) {
				// 非URL -> 作为资源路径解析
				return getResourceByPath(location);
			}
		}
	}

	/**
	 * 在给定路径上返回资源的资源句柄
	 * <p>默认实现支持类路径位置。这应该适用于独立的实现，但是可以被覆盖，例如针对Servlet容器的实现。
	 * @param path 资源路径
	 * @return 对应的资源句柄
	 * @see ClassPathResource
	 * @see org.springframework.context.support.FileSystemXmlApplicationContext#getResourceByPath
	 * @see org.springframework.web.context.support.XmlWebApplicationContext#getResourceByPath
	 */
	protected Resource getResourceByPath(String path) {
		return new ClassPathContextResource(path, getClassLoader());
	}


	/**
	 * ClassPathResource，通过实现ContextResource接口显式地表示上下文相关路径。
	 */
	protected static class ClassPathContextResource extends ClassPathResource implements ContextResource {

		public ClassPathContextResource(String path, @Nullable ClassLoader classLoader) {
			super(path, classLoader);
		}

		@Override
		public String getPathWithinContext() {
			return getPath();
		}

		@Override
		public Resource createRelative(String relativePath) {
			String pathToUse = StringUtils.applyRelativePath(getPath(), relativePath);
			return new ClassPathContextResource(pathToUse, getClassLoader());
		}
	}

}
