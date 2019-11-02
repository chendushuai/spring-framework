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

package org.springframework.context.annotation;

import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.core.type.MethodMetadata;

/**
 * Represents a {@link Configuration @Configuration} class method marked with the
 * {@link Bean @Bean} annotation.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see ConfigurationClass
 * @see ConfigurationClassParser
 * @see ConfigurationClassBeanDefinitionReader
 */
final class BeanMethod extends ConfigurationMethod {

	public BeanMethod(MethodMetadata metadata, ConfigurationClass configurationClass) {
		super(metadata, configurationClass);
	}

	@Override
	public void validate(ProblemReporter problemReporter) {
		if (getMetadata().isStatic()) {
			// 静态@Bean方法没有需要验证的约束—>立即返回
			return;
		}

		if (this.configurationClass.getMetadata().isAnnotated(Configuration.class.getName())) {
			if (!getMetadata().isOverridable()) {
				// 必须能够覆盖@Configuration类中的实例@Bean方法以适应CGLIB
				problemReporter.error(new NonOverridableMethodError());
			}
		}
	}


	private class NonOverridableMethodError extends Problem {

		public NonOverridableMethodError() {
			super(String.format("@Bean method '%s' must not be private or final; change the method's modifiers to continue",
					getMetadata().getMethodName()), getResourceLocation());
		}
	}
}
