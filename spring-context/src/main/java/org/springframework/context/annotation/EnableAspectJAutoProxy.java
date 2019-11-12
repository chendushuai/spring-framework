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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 支持处理带有AspectJ的{@code @Aspect}注释的组件，类似于Spring的{@code <aop:aspectj-autoproxy>} XML元素中的功能。
 * 用于@{@link Configuration}类，如下:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAspectJAutoProxy
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public FooService fooService() {
 *         return new FooService();
 *     }
 *
 *     &#064;Bean
 *     public MyAspect myAspect() {
 *         return new MyAspect();
 *     }
 * }</pre>
 *
 * 其中{@code FooService}是典型的POJO组件，{@code MyAspect}是{@code @Aspect}样式的切面:
 *
 * <pre class="code">
 * public class FooService {
 *
 *     // various methods
 * }</pre>
 *
 * <pre class="code">
 * &#064;Aspect
 * public class MyAspect {
 *
 *     &#064;Before("execution(* FooService+.*(..))")
 *     public void advice() {
 *         // advise FooService methods as appropriate
 *     }
 * }</pre>
 *
 * 在上面的场景中，{@code @EnableAspectJAutoProxy}确保{@code MyAspect}将被正确处理，{@code FooService}将混合它提供的建议。
 *
 * <p>用户可以使用{@link #proxyTargetClass()}属性控制为{@code FooService}创建的代理的类型。
 * 下面的代码启用了cglib风格的“子类”代理，而不是默认的基于接口的JDK代理方法。
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAspectJAutoProxy(proxyTargetClass=true)
 * public class AppConfig {
 *     // ...
 * }</pre>
 *
 * <p>注意，{@code @Aspect} bean可以像其他bean一样被组件扫描。简单地用{@code @Aspect}和{@code @Component}标记方面:
 *
 * <pre class="code">
 * package com.foo;
 *
 * &#064;Component
 * public class FooService { ... }
 *
 * &#064;Aspect
 * &#064;Component
 * public class MyAspect { ... }</pre>
 *
 * 然后使用@{@link ComponentScan}注解来挑选两个:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;ComponentScan("com.foo")
 * &#064;EnableAspectJAutoProxy
 * public class AppConfig {
 *
 *     // no explicit &#064Bean definitions required
 * }</pre>
 *
 * <b>注意:{@code @EnableAspectJAutoProxy}仅应用于其本地应用程序上下文，允许在不同级别上选择性地代理bean。</b>
 * 请在每个单独的上下文中重新声明{@code @EnableAspectJAutoProxy}，
 * 例如，公共根web应用程序上下文和任何单独的{@code DispatcherServlet}应用程序上下文，如果您需要在多个级别应用它的行为，请单独声明。
 *
 * <p>这个特性需要在类路径上存在{@code aspectjweaver}。
 * 虽然这种依赖关系对于{@code spring-aop}通常是可选的，但是对于{@code @EnableAspectJAutoProxy}及其基础设施却是必需的。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see org.aspectj.lang.annotation.Aspect
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AspectJAutoProxyRegistrar.class)
public @interface EnableAspectJAutoProxy {

	/**
	 * 指示是否创建基于子类(CGLIB)的代理，而不是标准的基于Java接口的代理。
	 * 默认是{@code false}。
	 */
	boolean proxyTargetClass() default false;

	/**
	 * 指出AOP框架应该将代理公开为{@code ThreadLocal}，以便通过{@link org.springframework.aop.framework.AopContext}类检索。
	 * 默认情况下关闭，也就是说不能保证{@code AopContext}访问可以工作。
	 * @since 4.3.1
	 */
	boolean exposeProxy() default false;

}
