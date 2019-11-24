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

package org.springframework.web.context.request;

import java.security.Principal;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.springframework.lang.Nullable;

/**
 * Generic interface for a web request. Mainly intended for generic web
 * request interceptors, giving them access to general request metadata,
 * not for actual handling of the request.
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @since 2.0
 * @see WebRequestInterceptor
 */
public interface WebRequest extends RequestAttributes {

	/**
	 * Return the request header of the given name, or {@code null} if none.
	 * <p>Retrieves the first header value in case of a multi-value header.
	 * @since 3.0
	 * @see javax.servlet.http.HttpServletRequest#getHeader(String)
	 */
	@Nullable
	String getHeader(String headerName);

	/**
	 * Return the request header values for the given header name,
	 * or {@code null} if none.
	 * <p>A single-value header will be exposed as an array with a single element.
	 * @since 3.0
	 * @see javax.servlet.http.HttpServletRequest#getHeaders(String)
	 */
	@Nullable
	String[] getHeaderValues(String headerName);

	/**
	 * Return a Iterator over request header names.
	 * @since 3.0
	 * @see javax.servlet.http.HttpServletRequest#getHeaderNames()
	 */
	Iterator<String> getHeaderNames();

	/**
	 * Return the request parameter of the given name, or {@code null} if none.
	 * <p>Retrieves the first parameter value in case of a multi-value parameter.
	 * @see javax.servlet.http.HttpServletRequest#getParameter(String)
	 */
	@Nullable
	String getParameter(String paramName);

	/**
	 * Return the request parameter values for the given parameter name,
	 * or {@code null} if none.
	 * <p>A single-value parameter will be exposed as an array with a single element.
	 * @see javax.servlet.http.HttpServletRequest#getParameterValues(String)
	 */
	@Nullable
	String[] getParameterValues(String paramName);

	/**
	 * Return a Iterator over request parameter names.
	 * @since 3.0
	 * @see javax.servlet.http.HttpServletRequest#getParameterNames()
	 */
	Iterator<String> getParameterNames();

	/**
	 * Return a immutable Map of the request parameters, with parameter names as map keys
	 * and parameter values as map values. The map values will be of type String array.
	 * <p>A single-value parameter will be exposed as an array with a single element.
	 * @see javax.servlet.http.HttpServletRequest#getParameterMap()
	 */
	Map<String, String[]> getParameterMap();

	/**
	 * Return the primary Locale for this request.
	 * @see javax.servlet.http.HttpServletRequest#getLocale()
	 */
	Locale getLocale();

	/**
	 * Return the context path for this request
	 * (usually the base path that the current web application is mapped to).
	 * @see javax.servlet.http.HttpServletRequest#getContextPath()
	 */
	String getContextPath();

	/**
	 * Return the remote user for this request, if any.
	 * @see javax.servlet.http.HttpServletRequest#getRemoteUser()
	 */
	@Nullable
	String getRemoteUser();

	/**
	 * Return the user principal for this request, if any.
	 * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
	 */
	@Nullable
	Principal getUserPrincipal();

	/**
	 * Determine whether the user is in the given role for this request.
	 * @see javax.servlet.http.HttpServletRequest#isUserInRole(String)
	 */
	boolean isUserInRole(String role);

	/**
	 * Return whether this request has been sent over a secure transport
	 * mechanism (such as SSL).
	 * @see javax.servlet.http.HttpServletRequest#isSecure()
	 */
	boolean isSecure();

	/**
	 * 根据提供的last-modified时间戳(由应用程序确定)，检查请求的资源是否已被修改。
	 * <p>这也将透明地设置“Last-Modified”响应头和HTTP状态(如果适用)。
	 * <p>使用示范:
	 * <pre class="code">
	 * public String myHandleMethod(WebRequest webRequest, Model model) {
	 *   long lastModified = // application-specific calculation
	 *   if (request.checkNotModified(lastModified)) {
	 *     // shortcut exit - no further processing necessary
	 *     return null;
	 *   }
	 *   // further request processing, actually building content
	 *   model.addAttribute(...);
	 *   return "myViewName";
	 * }</pre>
	 * <p>此方法适用于有条件的GET/HEAD请求，也适用于有条件的POST/PUT/DELETE请求。
	 * <p><strong>注意:</strong> 您可以使用这个{@code #checkNotModified(long)}方法;
	 * 或{@link #checkNotModified(String)}方法。
	 * 如果您希望同时强制一个强实体标记和一个Last-Modified值，就像HTTP规范建议的那样，
	 * 那么您应该使用{@link #checkNotModified(String, long)}。
	 * <p>如果设置了“If-Modified-Since”标头，但不能解析为日期值，
	 * 则此方法将忽略标头，并继续设置响应上的last-modified时间戳。
	 * @param lastModifiedTimestamp 应用程序为底层资源确定的last-modified的时间戳(以毫秒为单位)
	 * @return 请求是否符合未修改的条件，允许中止请求处理并依赖于通知客户机内容未修改的响应
	 */
	boolean checkNotModified(long lastModifiedTimestamp);

	/**
	 * Check whether the requested resource has been modified given the
	 * supplied {@code ETag} (entity tag), as determined by the application.
	 * <p>This will also transparently set the "ETag" response header
	 * and HTTP status when applicable.
	 * <p>Typical usage:
	 * <pre class="code">
	 * public String myHandleMethod(WebRequest webRequest, Model model) {
	 *   String eTag = // application-specific calculation
	 *   if (request.checkNotModified(eTag)) {
	 *     // shortcut exit - no further processing necessary
	 *     return null;
	 *   }
	 *   // further request processing, actually building content
	 *   model.addAttribute(...);
	 *   return "myViewName";
	 * }</pre>
	 * <p><strong>Note:</strong> you can use either
	 * this {@code #checkNotModified(String)} method; or
	 * {@link #checkNotModified(long)}. If you want enforce both
	 * a strong entity tag and a Last-Modified value,
	 * as recommended by the HTTP specification,
	 * then you should use {@link #checkNotModified(String, long)}.
	 * @param etag the entity tag that the application determined
	 * for the underlying resource. This parameter will be padded
	 * with quotes (") if necessary.
	 * @return true if the request does not require further processing.
	 */
	boolean checkNotModified(String etag);

	/**
	 * Check whether the requested resource has been modified given the
	 * supplied {@code ETag} (entity tag) and last-modified timestamp,
	 * as determined by the application.
	 * <p>This will also transparently set the "ETag" and "Last-Modified"
	 * response headers, and HTTP status when applicable.
	 * <p>Typical usage:
	 * <pre class="code">
	 * public String myHandleMethod(WebRequest webRequest, Model model) {
	 *   String eTag = // application-specific calculation
	 *   long lastModified = // application-specific calculation
	 *   if (request.checkNotModified(eTag, lastModified)) {
	 *     // shortcut exit - no further processing necessary
	 *     return null;
	 *   }
	 *   // further request processing, actually building content
	 *   model.addAttribute(...);
	 *   return "myViewName";
	 * }</pre>
	 * <p>This method works with conditional GET/HEAD requests, but
	 * also with conditional POST/PUT/DELETE requests.
	 * <p><strong>Note:</strong> The HTTP specification recommends
	 * setting both ETag and Last-Modified values, but you can also
	 * use {@code #checkNotModified(String)} or
	 * {@link #checkNotModified(long)}.
	 * @param etag the entity tag that the application determined
	 * for the underlying resource. This parameter will be padded
	 * with quotes (") if necessary.
	 * @param lastModifiedTimestamp the last-modified timestamp in
	 * milliseconds that the application determined for the underlying
	 * resource
	 * @return true if the request does not require further processing.
	 * @since 4.2
	 */
	boolean checkNotModified(@Nullable String etag, long lastModifiedTimestamp);

	/**
	 * Get a short description of this request,
	 * typically containing request URI and session id.
	 * @param includeClientInfo whether to include client-specific
	 * information such as session id and user name
	 * @return the requested description as String
	 */
	String getDescription(boolean includeClientInfo);

}
