/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.multipart;

import javax.servlet.http.HttpServletRequest;

/**
 * 根据<a href="https://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a>实现
 * 通常可以在应用程序上下文中使用，也可以单独使用。
 *
 * <p>在Spring 3.1中有两个具体的实现:
 * <ul>
 * <li>{@link org.springframework.web.multipart.commons.CommonsMultipartResolver}
 * 用于Apache Commons FileUpload
 * <li>{@link org.springframework.web.multipart.support.StandardServletMultipartResolver}
 * 对于Servlet 3.0+部分API
 * </ul>
 *
 * <p>没有用于Spring {@link org.springframework.web.servlet.DispatcherServlet DispatcherServlets}的默认解析器实现，
 * 因为应用程序可能选择解析其自身的多部分请求。
 * 要定义一个实现，在{@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}的应用程序上下文
 * 中创建一个id为“multipartResolver”的bean。
 * 这样的解析器应用于由{@link org.springframework.web.servlet.DispatcherServlet}处理的所有请求。
 *
 * <p>如果一个{@link org.springframework.web.servlet.DispatcherServlet}检测到一个多部分请求，
 * 它将通过配置的{@link MultipartResolver}解析它，并传递一个包装的{@link javax.servlet.http.HttpServletRequest}。
 * 然后，控制器可以将其给定的请求转换为{@link MultipartHttpServletRequest}接口，
 * 该接口允许访问任何{@link MultipartFile MultipartFiles}。
 * 请注意，只有在实际的多部分请求时才支持这种强制转换。
 *
 * <pre class="code">
 * public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) {
 *   MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
 *   MultipartFile multipartFile = multipartRequest.getFile("image");
 *   ...
 * }</pre>
 *
 * 命令或表单控制器可以注册一个{@link org.springframework.web.multipart.support.ByteArrayMultipartFileEditor}
 * 或{@link org.springframework.web.multipart.support.StringMultipartFileEditor}及其数据绑定，而不是直接访问，
 * 以自动将多部分内容应用于表单bean属性。
 *
 * <p>作为使用{@link MultipartResolver}和{@link org.springframework.web.servlet.DispatcherServlet}的替代方法，
 * {@link org.springframework.web.multipart.support.MultipartFilter}可以在{@code web.xml}中注册。
 * 它将委托给根应用程序上下文中相应的{@link MultipartResolver} bean。
 * 这主要用于不使用Spring自己的web MVC框架的应用程序。
 *
 * <p>注意:几乎不需要从应用程序代码访问{@link MultipartResolver}本身。
 * 它只是在后台工作，使{@link MultipartHttpServletRequest MultipartHttpServletRequests}可用于控制器。
 *
 * @author Juergen Hoeller
 * @author Trevor D. Cook
 * @since 29.09.2003
 * @see MultipartHttpServletRequest
 * @see MultipartFile
 * @see org.springframework.web.multipart.commons.CommonsMultipartResolver
 * @see org.springframework.web.multipart.support.ByteArrayMultipartFileEditor
 * @see org.springframework.web.multipart.support.StringMultipartFileEditor
 * @see org.springframework.web.servlet.DispatcherServlet
 */
public interface MultipartResolver {

	/**
	 * 确定给定的请求是否包含多部分内容。
	 * <p>通常会检查内容类型“multipart/form-data”，但实际接受的请求可能取决于解析器实现的功能。
	 * @param request 要评估的servlet请求
	 * @return 请求是否包含多部分内容
	 */
	boolean isMultipart(HttpServletRequest request);

	/**
	 * Parse the given HTTP request into multipart files and parameters,
	 * and wrap the request inside a
	 * {@link org.springframework.web.multipart.MultipartHttpServletRequest}
	 * object that provides access to file descriptors and makes contained
	 * parameters accessible via the standard ServletRequest methods.
	 * @param request the servlet request to wrap (must be of a multipart content type)
	 * @return the wrapped servlet request
	 * @throws MultipartException if the servlet request is not multipart, or if
	 * implementation-specific problems are encountered (such as exceeding file size limits)
	 * @see MultipartHttpServletRequest#getFile
	 * @see MultipartHttpServletRequest#getFileNames
	 * @see MultipartHttpServletRequest#getFileMap
	 * @see javax.servlet.http.HttpServletRequest#getParameter
	 * @see javax.servlet.http.HttpServletRequest#getParameterNames
	 * @see javax.servlet.http.HttpServletRequest#getParameterMap
	 */
	MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException;

	/**
	 * Cleanup any resources used for the multipart handling,
	 * like a storage for the uploaded files.
	 * @param request the request to cleanup resources for
	 */
	void cleanupMultipart(MultipartHttpServletRequest request);

}
