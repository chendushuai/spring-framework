/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.core.type.classreading;

import java.io.IOException;

import org.springframework.core.io.Resource;

/**
 * 用于{@link MetadataReader}实例的工厂接口。允许缓存每个原始资源的元数据阅读器。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see SimpleMetadataReaderFactory
 * @see CachingMetadataReaderFactory
 */
public interface MetadataReaderFactory {

	/**
	 * 为给定的类名获取元数据阅读器。
	 * @param className 类名 (要解析为一个".class"文件)
	 * @return ClassReader实现的holder (不可能是 {@code null})
	 * @throws IOException I/O失败的情况
	 */
	MetadataReader getMetadataReader(String className) throws IOException;

	/**
	 * 获取给定资源的元数据阅读器。
	 * @param resource 资源(指向一个".class"文件)
	 * @return ClassReader实现的holder (不可能是 {@code null})
	 * @throws IOException I/O失败的情况
	 */
	MetadataReader getMetadataReader(Resource resource) throws IOException;

}
