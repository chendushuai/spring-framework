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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;

/**
 * 空bean实例的内部表示，例如，对于从{@link FactoryBean#getObject()}或工厂方法返回的{@code null}值。
 *
 * <p>每个这样的空bean都由一个专用的{@code NullBean}实例表示，该实例彼此不相等，
 * 惟一地将返回的每个bean与{@link org.springframework.beans.factory.BeanFactory#getBean}的所有变体区分开来。
 * 但是，对于{@code #equals(null)}，每个这样的实例都将返回{@code true}，
 * 并从{@code #toString()}返回“null”，这是可以在外部测试它们的方法(因为该类本身不是公共的)。
 *
 * @author Juergen Hoeller
 * @since 5.0
 */
final class NullBean {

	NullBean() {
	}


	@Override
	public boolean equals(@Nullable Object obj) {
		return (this == obj || obj == null);
	}

	@Override
	public int hashCode() {
		return NullBean.class.hashCode();
	}

	@Override
	public String toString() {
		return "null";
	}

}
