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

package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;

/**
 * MVC框架SPI，允许参数化的核心MVC工作流。
 *
 * <p>必须为每个处理程序类型实现的接口来处理请求。
 * 这个接口允许{@link DispatcherServlet}无限扩展。
 * {@code DispatcherServlet}通过这个接口访问所有安装的处理程序，这意味着它不包含特定于任何处理程序类型的代码。
 *
 * <p>注意，处理程序的类型可以是{@code Object}。
 * 这是为了使来自其他框架的处理程序能够与此框架集成，而无需自定义编码，并允许不遵循任何特定Java接口的注释驱动的处理程序对象。
 *
 * <p>此接口不适合应用程序开发人员使用。
 * 它可用于想要开发自己的web工作流的处理程序。
 *
 * <p>注意:{@code HandlerAdapter}实现者可以实现{@link org.springframework.core.Ordered}
 * 接口来指定一个排序顺序(因此是一个优先级)，以便由{@code DispatcherServlet}应用。
 * 无序实例被视为最低优先级。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter
 * @see org.springframework.web.servlet.handler.SimpleServletHandlerAdapter
 */
public interface HandlerAdapter {

	/**
	 * 给定一个处理程序实例，返回这个{@code HandlerAdapter}是否支持它。
	 * 典型的HandlerAdapters将根据处理程序类型进行决策。HandlerAdapters通常只支持一种处理程序类型。
	 * <p>一个典型的实现:
	 * <p>{@code
	 * return (handler instanceof MyHandler);
	 * }
	 * @param handler 要检查的处理程序对象
	 * @return 此对象是否可以使用给定的处理程序
	 */
	boolean supports(Object handler);

	/**
	 * 使用给定的处理程序处理此请求。所需的工作流程可能差异很大。
	 * @param request 当前 HTTP request
	 * @param response 当前 HTTP response
	 * @param handler 要使用的处理程序。这个对象之前必须传递给这个接口的{@code supports}方法，该方法必须返回{@code true}。
	 * @throws Exception 以防出错
	 * @return 一个ModelAndView对象，具有视图的名称和所需的模型数据，或者{@code null}(如果直接处理了请求)
	 */
	@Nullable
	ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception;

	/**
	 * 与HttpServlet的{@code getLastModified}方法相同。
	 * 如果处理程序类中没有支持，可以简单地返回-1。
	 * @param request 当前 HTTP request
	 * @param handler 要使用的处理程序
	 * @return 给定处理程序的lastModified值
	 * @see javax.servlet.http.HttpServlet#getLastModified
	 * @see org.springframework.web.servlet.mvc.LastModified#getLastModified
	 */
	long getLastModified(HttpServletRequest request, Object handler);

}
