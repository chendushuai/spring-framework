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

package org.springframework.beans;

import org.springframework.lang.Nullable;

/**
 * 接口由携带配置源对象的bean元数据元素实现。
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
public interface BeanMetadataElement {

	/**
	 * 返回这个元数据元素的配置源{@code Object}(可能是{@code null})。
	 */
	@Nullable
	default Object getSource() {
		return null;
	}

}
