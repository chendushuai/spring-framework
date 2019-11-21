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

package org.springframework.web.servlet.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * 将该注解添加到{@code @Configuration}类中，将从{@link WebMvcConfigurationSupport}中导入Spring MVC配置，例如:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebMvc
 * &#064;ComponentScan(basePackageClasses = MyConfiguration.class)
 * public class MyConfiguration {
 *
 * }
 * </pre>
 *
 * <p>要自定义导入的配置，请实现接口{@link WebMvcConfigurer}并覆盖各个方法，例如:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebMvc
 * &#064;ComponentScan(basePackageClasses = MyConfiguration.class)
 * public class MyConfiguration implements WebMvcConfigurer {
 *
 * 	   &#064;Override
 * 	   public void addFormatters(FormatterRegistry formatterRegistry) {
 *         formatterRegistry.addConverter(new MyConverter());
 * 	   }
 *
 * 	   &#064;Override
 * 	   public void configureMessageConverters(List&lt;HttpMessageConverter&lt;?&gt;&gt; converters) {
 *         converters.add(new MyHttpMessageConverter());
 * 	   }
 *
 * }
 * </pre>
 *
 * <p><strong>注意:</strong>只有一个{@code @Configuration}类可能具有{@code @EnableWebMvc}注释来导入Spring Web MVC配置。
 * 但是，可以有多个{@code @Configuration}类实现{@code WebMvcConfigurer}来定制提供的配置。
 *
 * <p>如果{@link WebMvcConfigurer}没有暴露一些需要配置的更高级的设置，考虑删除{@code @EnableWebMvc}注解，
 * 直接从{@link WebMvcConfigurationSupport}或{@link DelegatingWebMvcConfiguration}扩展，例如:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;ComponentScan(basePackageClasses = { MyConfiguration.class })
 * public class MyConfiguration extends WebMvcConfigurationSupport {
 *
 * 	   &#064;Override
 *	   public void addFormatters(FormatterRegistry formatterRegistry) {
 *         formatterRegistry.addConverter(new MyConverter());
 *	   }
 *
 *	   &#064;Bean
 *	   public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
 *         // Create or delegate to "super" to create and
 *         // customize properties of RequestMappingHandlerAdapter
 *	   }
 * }
 * </pre>
 *
 * @author Dave Syer
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurer
 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport
 * @see org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(DelegatingWebMvcConfiguration.class)
public @interface EnableWebMvc {
}
