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

package org.springframework.web.servlet.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsProcessor;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UrlPathHelper;

/**
 * Abstract base class for {@link org.springframework.web.servlet.HandlerMapping}
 * implementations. Supports ordering, a default handler, handler interceptors,
 * including handler interceptors mapped by path patterns.
 *
 * <p>Note: This base class does <i>not</i> support exposure of the
 * {@link #PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE}. Support for this attribute
 * is up to concrete subclasses, typically based on request URL mappings.
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 07.04.2003
 * @see #getHandlerInternal
 * @see #setDefaultHandler
 * @see #setAlwaysUseFullPath
 * @see #setUrlDecode
 * @see org.springframework.util.AntPathMatcher
 * @see #setInterceptors
 * @see org.springframework.web.servlet.HandlerInterceptor
 */
public abstract class AbstractHandlerMapping extends WebApplicationObjectSupport
		implements HandlerMapping, Ordered, BeanNameAware {

	@Nullable
	private Object defaultHandler;

	private UrlPathHelper urlPathHelper = new UrlPathHelper();

	private PathMatcher pathMatcher = new AntPathMatcher();

	private final List<Object> interceptors = new ArrayList<>();

	private final List<HandlerInterceptor> adaptedInterceptors = new ArrayList<>();

	@Nullable
	private CorsConfigurationSource corsConfigurationSource;

	private CorsProcessor corsProcessor = new DefaultCorsProcessor();

	private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered

	@Nullable
	private String beanName;


	/**
	 * Set the default handler for this handler mapping.
	 * This handler will be returned if no specific mapping was found.
	 * <p>Default is {@code null}, indicating no default handler.
	 */
	public void setDefaultHandler(@Nullable Object defaultHandler) {
		this.defaultHandler = defaultHandler;
	}

	/**
	 * Return the default handler for this handler mapping,
	 * or {@code null} if none.
	 */
	@Nullable
	public Object getDefaultHandler() {
		return this.defaultHandler;
	}

	/**
	 * Shortcut to same property on underlying {@link #setUrlPathHelper UrlPathHelper}.
	 * @see org.springframework.web.util.UrlPathHelper#setAlwaysUseFullPath(boolean)
	 */
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
			((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setAlwaysUseFullPath(alwaysUseFullPath);
		}
	}

	/**
	 * Shortcut to same property on underlying {@link #setUrlPathHelper UrlPathHelper}.
	 * @see org.springframework.web.util.UrlPathHelper#setUrlDecode(boolean)
	 */
	public void setUrlDecode(boolean urlDecode) {
		this.urlPathHelper.setUrlDecode(urlDecode);
		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
			((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setUrlDecode(urlDecode);
		}
	}

	/**
	 * Shortcut to same property on underlying {@link #setUrlPathHelper UrlPathHelper}.
	 * @see org.springframework.web.util.UrlPathHelper#setRemoveSemicolonContent(boolean)
	 */
	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
		this.urlPathHelper.setRemoveSemicolonContent(removeSemicolonContent);
		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
			((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setRemoveSemicolonContent(removeSemicolonContent);
		}
	}

	/**
	 * Set the UrlPathHelper to use for resolution of lookup paths.
	 * <p>Use this to override the default UrlPathHelper with a custom subclass,
	 * or to share common UrlPathHelper settings across multiple HandlerMappings
	 * and MethodNameResolvers.
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
			((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setUrlPathHelper(urlPathHelper);
		}
	}

	/**
	 * Return the UrlPathHelper implementation to use for resolution of lookup paths.
	 */
	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}

	/**
	 * Set the PathMatcher implementation to use for matching URL paths
	 * against registered URL patterns. Default is AntPathMatcher.
	 * @see org.springframework.util.AntPathMatcher
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
		if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
			((UrlBasedCorsConfigurationSource) this.corsConfigurationSource).setPathMatcher(pathMatcher);
		}
	}

	/**
	 * Return the PathMatcher implementation to use for matching URL paths
	 * against registered URL patterns.
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * Set the interceptors to apply for all handlers mapped by this handler mapping.
	 * <p>Supported interceptor types are HandlerInterceptor, WebRequestInterceptor, and MappedInterceptor.
	 * Mapped interceptors apply only to request URLs that match its path patterns.
	 * Mapped interceptor beans are also detected by type during initialization.
	 * @param interceptors array of handler interceptors
	 * @see #adaptInterceptor
	 * @see org.springframework.web.servlet.HandlerInterceptor
	 * @see org.springframework.web.context.request.WebRequestInterceptor
	 */
	public void setInterceptors(Object... interceptors) {
		this.interceptors.addAll(Arrays.asList(interceptors));
	}

	/**
	 * Set the "global" CORS configurations based on URL patterns. By default the first
	 * matching URL pattern is combined with the CORS configuration for the handler, if any.
	 * @since 4.2
	 * @see #setCorsConfigurationSource(CorsConfigurationSource)
	 */
	public void setCorsConfigurations(Map<String, CorsConfiguration> corsConfigurations) {
		Assert.notNull(corsConfigurations, "corsConfigurations must not be null");
		if (!corsConfigurations.isEmpty()) {
			UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
			source.setCorsConfigurations(corsConfigurations);
			source.setPathMatcher(this.pathMatcher);
			source.setUrlPathHelper(this.urlPathHelper);
			source.setLookupPathAttributeName(LOOKUP_PATH);
			this.corsConfigurationSource = source;
		}
		else {
			this.corsConfigurationSource = null;
		}
	}

	/**
	 * Set the "global" CORS configuration source. By default the first matching URL
	 * pattern is combined with the CORS configuration for the handler, if any.
	 * @since 5.1
	 * @see #setCorsConfigurations(Map)
	 */
	public void setCorsConfigurationSource(CorsConfigurationSource corsConfigurationSource) {
		Assert.notNull(corsConfigurationSource, "corsConfigurationSource must not be null");
		this.corsConfigurationSource = corsConfigurationSource;
	}

	/**
	 * Configure a custom {@link CorsProcessor} to use to apply the matched
	 * {@link CorsConfiguration} for a request.
	 * <p>By default {@link DefaultCorsProcessor} is used.
	 * @since 4.2
	 */
	public void setCorsProcessor(CorsProcessor corsProcessor) {
		Assert.notNull(corsProcessor, "CorsProcessor must not be null");
		this.corsProcessor = corsProcessor;
	}

	/**
	 * Return the configured {@link CorsProcessor}.
	 */
	public CorsProcessor getCorsProcessor() {
		return this.corsProcessor;
	}

	/**
	 * Specify the order value for this HandlerMapping bean.
	 * <p>The default value is {@code Ordered.LOWEST_PRECEDENCE}, meaning non-ordered.
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	protected String formatMappingName() {
		return this.beanName != null ? "'" + this.beanName + "'" : "<unknown>";
	}


	/**
	 * Initializes the interceptors.
	 * @see #extendInterceptors(java.util.List)
	 * @see #initInterceptors()
	 */
	@Override
	protected void initApplicationContext() throws BeansException {
		extendInterceptors(this.interceptors);
		detectMappedInterceptors(this.adaptedInterceptors);
		initInterceptors();
	}

	/**
	 * Extension hook that subclasses can override to register additional interceptors,
	 * given the configured interceptors (see {@link #setInterceptors}).
	 * <p>Will be invoked before {@link #initInterceptors()} adapts the specified
	 * interceptors into {@link HandlerInterceptor} instances.
	 * <p>The default implementation is empty.
	 * @param interceptors the configured interceptor List (never {@code null}), allowing
	 * to add further interceptors before as well as after the existing interceptors
	 */
	protected void extendInterceptors(List<Object> interceptors) {
	}

	/**
	 * Detect beans of type {@link MappedInterceptor} and add them to the list of mapped interceptors.
	 * <p>This is called in addition to any {@link MappedInterceptor MappedInterceptors} that may have been provided
	 * via {@link #setInterceptors}, by default adding all beans of type {@link MappedInterceptor}
	 * from the current context and its ancestors. Subclasses can override and refine this policy.
	 * @param mappedInterceptors an empty list to add {@link MappedInterceptor} instances to
	 */
	protected void detectMappedInterceptors(List<HandlerInterceptor> mappedInterceptors) {
		mappedInterceptors.addAll(
				BeanFactoryUtils.beansOfTypeIncludingAncestors(
						obtainApplicationContext(), MappedInterceptor.class, true, false).values());
	}

	/**
	 * Initialize the specified interceptors, checking for {@link MappedInterceptor MappedInterceptors} and
	 * adapting {@link HandlerInterceptor}s and {@link WebRequestInterceptor HandlerInterceptor}s and
	 * {@link WebRequestInterceptor}s if necessary.
	 * @see #setInterceptors
	 * @see #adaptInterceptor
	 */
	protected void initInterceptors() {
		if (!this.interceptors.isEmpty()) {
			for (int i = 0; i < this.interceptors.size(); i++) {
				Object interceptor = this.interceptors.get(i);
				if (interceptor == null) {
					throw new IllegalArgumentException("Entry number " + i + " in interceptors array is null");
				}
				this.adaptedInterceptors.add(adaptInterceptor(interceptor));
			}
		}
	}

	/**
	 * Adapt the given interceptor object to the {@link HandlerInterceptor} interface.
	 * <p>By default, the supported interceptor types are {@link HandlerInterceptor}
	 * and {@link WebRequestInterceptor}. Each given {@link WebRequestInterceptor}
	 * will be wrapped in a {@link WebRequestHandlerInterceptorAdapter}.
	 * Can be overridden in subclasses.
	 * @param interceptor the specified interceptor object
	 * @return the interceptor wrapped as HandlerInterceptor
	 * @see org.springframework.web.servlet.HandlerInterceptor
	 * @see org.springframework.web.context.request.WebRequestInterceptor
	 * @see WebRequestHandlerInterceptorAdapter
	 */
	protected HandlerInterceptor adaptInterceptor(Object interceptor) {
		if (interceptor instanceof HandlerInterceptor) {
			return (HandlerInterceptor) interceptor;
		}
		else if (interceptor instanceof WebRequestInterceptor) {
			return new WebRequestHandlerInterceptorAdapter((WebRequestInterceptor) interceptor);
		}
		else {
			throw new IllegalArgumentException("Interceptor type not supported: " + interceptor.getClass().getName());
		}
	}

	/**
	 * Return the adapted interceptors as {@link HandlerInterceptor} array.
	 * @return the array of {@link HandlerInterceptor HandlerInterceptors}, or {@code null} if none
	 */
	@Nullable
	protected final HandlerInterceptor[] getAdaptedInterceptors() {
		return (!this.adaptedInterceptors.isEmpty() ?
				this.adaptedInterceptors.toArray(new HandlerInterceptor[0]) : null);
	}

	/**
	 * Return all configured {@link MappedInterceptor MappedInterceptors} as an array.
	 * @return the array of {@link MappedInterceptor MappedInterceptors}, or {@code null} if none
	 */
	@Nullable
	protected final MappedInterceptor[] getMappedInterceptors() {
		List<MappedInterceptor> mappedInterceptors = new ArrayList<>(this.adaptedInterceptors.size());
		for (HandlerInterceptor interceptor : this.adaptedInterceptors) {
			if (interceptor instanceof MappedInterceptor) {
				mappedInterceptors.add((MappedInterceptor) interceptor);
			}
		}
		return (!mappedInterceptors.isEmpty() ? mappedInterceptors.toArray(new MappedInterceptor[0]) : null);
	}


	/**
	 * 查找给定请求的处理程序，如果没有找到特定的处理程序，则返回到默认处理程序。
	 * @param request 当前 HTTP 请求
	 * @return 对应处理程序实例，或默认处理程序
	 * @see #getHandlerInternal
	 */
	@Override
	@Nullable
	public final HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		Object handler = getHandlerInternal(request);
		if (handler == null) {
			handler = getDefaultHandler();
		}
		if (handler == null) {
			return null;
		}
		// Bean名称或已解析的处理程序?
		if (handler instanceof String) {
			String handlerName = (String) handler;
			handler = obtainApplicationContext().getBean(handlerName);
		}

		HandlerExecutionChain executionChain = getHandlerExecutionChain(handler, request);

		if (logger.isTraceEnabled()) {
			logger.trace("Mapped to " + handler);
		}
		else if (logger.isDebugEnabled() && !request.getDispatcherType().equals(DispatcherType.ASYNC)) {
			logger.debug("Mapped to " + executionChain.getHandler());
		}

		// 判断处理程序有没有实现CorsConfigurationSource接口
		if (hasCorsConfigurationSource(handler)) {
			CorsConfiguration config = (this.corsConfigurationSource != null ? this.corsConfigurationSource.getCorsConfiguration(request) : null);
			// 得到跨域CROS配置
			CorsConfiguration handlerConfig = getCorsConfiguration(handler, request);
			config = (config != null ? config.combine(handlerConfig) : handlerConfig);
			executionChain = getCorsHandlerExecutionChain(request, executionChain, config);
		}

		return executionChain;
	}

	/**
	 * Look up a handler for the given request, returning {@code null} if no
	 * specific one is found. This method is called by {@link #getHandler};
	 * a {@code null} return value will lead to the default handler, if one is set.
	 * <p>On CORS pre-flight requests this method should return a match not for
	 * the pre-flight request but for the expected actual request based on the URL
	 * path, the HTTP methods from the "Access-Control-Request-Method" header, and
	 * the headers from the "Access-Control-Request-Headers" header thus allowing
	 * the CORS configuration to be obtained via {@link #getCorsConfiguration(Object, HttpServletRequest)},
	 * <p>Note: This method may also return a pre-built {@link HandlerExecutionChain},
	 * combining a handler object with dynamically determined interceptors.
	 * Statically specified interceptors will get merged into such an existing chain.
	 * @param request current HTTP request
	 * @return the corresponding handler instance, or {@code null} if none found
	 * @throws Exception if there is an internal error
	 */
	@Nullable
	protected abstract Object getHandlerInternal(HttpServletRequest request) throws Exception;

	/**
	 * 为给定的处理程序(包括适用的拦截器)构建{@link HandlerExecutionChain}。
	 * <p>默认实现使用给定的处理程序、处理程序映射的公共拦截器和与当前请求URL匹配的任何
	 * {@link MappedInterceptor MappedInterceptors}构建标准{@link HandlerExecutionChain}。
	 * 拦截器是按照注册的顺序添加的。子类可以覆盖它来扩展/重新排列拦截器列表。
	 * <p><b>注意:</b>传入的处理程序对象可以是原始处理程序或预先构建的{@link HandlerExecutionChain}。
	 * 这个方法应该显式地处理这两种情况，要么构建一个新的{@link HandlerExecutionChain}，要么扩展现有的链。
	 * <p>对于简单地在自定义子类中添加拦截器，可以考虑调用{@code super.getHandlerExecutionChain(handler, request)}
	 * 并且在返回的chain对象上调用{@link HandlerExecutionChain#addInterceptor}。
	 * @param handler 解析的处理程序实例(绝不是{@code null})
	 * @param request 当前 HTTP 请求
	 * @return HandlerExecutionChain (绝不是 {@code null})
	 * @see #getAdaptedInterceptors()
	 */
	protected HandlerExecutionChain getHandlerExecutionChain(Object handler, HttpServletRequest request) {
		HandlerExecutionChain chain = (handler instanceof HandlerExecutionChain ?
				(HandlerExecutionChain) handler : new HandlerExecutionChain(handler));

		// 得到处理后的路径地址
		String lookupPath = this.urlPathHelper.getLookupPathForRequest(request, LOOKUP_PATH);
		for (HandlerInterceptor interceptor : this.adaptedInterceptors) {
			if (interceptor instanceof MappedInterceptor) {
				MappedInterceptor mappedInterceptor = (MappedInterceptor) interceptor;
				if (mappedInterceptor.matches(lookupPath, this.pathMatcher)) {
					chain.addInterceptor(mappedInterceptor.getInterceptor());
				}
			}
			else {
				chain.addInterceptor(interceptor);
			}
		}
		return chain;
	}

	/**
	 * 如果这个处理程序有实现{@link CorsConfigurationSource}接口，那么返回{@code true}。
	 * @since 5.2
	 */
	protected boolean hasCorsConfigurationSource(Object handler) {
		return handler instanceof CorsConfigurationSource || this.corsConfigurationSource != null;
	}

	/**
	 * 检索给定处理程序的CORS配置。
	 * @param handler 要检查的处理程序(绝不是 {@code null}).
	 * @param request 当前请求
	 * @return 处理程序的CORS配置，如果没有，则为{@code null}
	 * @since 4.2
	 */
	@Nullable
	protected CorsConfiguration getCorsConfiguration(Object handler, HttpServletRequest request) {
		Object resolvedHandler = handler;
		if (handler instanceof HandlerExecutionChain) {
			resolvedHandler = ((HandlerExecutionChain) handler).getHandler();
		}
		if (resolvedHandler instanceof CorsConfigurationSource) {
			return ((CorsConfigurationSource) resolvedHandler).getCorsConfiguration(request);
		}
		return null;
	}

	/**
	 * 更新与CORS相关的处理的HandlerExecutionChain。
	 * <p>对于预校准请求，默认实现使用一个简单的HttpRequestHandler替换所选的处理程序，
	 * 该处理程序调用所配置的{@link #setCorsProcessor}。
	 * <p>对于实际的请求，默认的实现将插入一个HandlerInterceptor，该拦截器执行与CORS相关的检查并添加CORS标头。
	 * @param request the current request
	 * @param chain the handler chain
	 * @param config the applicable CORS configuration (possibly {@code null})
	 * @since 4.2
	 */
	protected HandlerExecutionChain getCorsHandlerExecutionChain(HttpServletRequest request,
			HandlerExecutionChain chain, @Nullable CorsConfiguration config) {

		if (CorsUtils.isPreFlightRequest(request)) {
			HandlerInterceptor[] interceptors = chain.getInterceptors();
			chain = new HandlerExecutionChain(new PreFlightHandler(config), interceptors);
		}
		else {
			chain.addInterceptor(0, new CorsInterceptor(config));
		}
		return chain;
	}


	private class PreFlightHandler implements HttpRequestHandler, CorsConfigurationSource {

		@Nullable
		private final CorsConfiguration config;

		public PreFlightHandler(@Nullable CorsConfiguration config) {
			this.config = config;
		}

		@Override
		public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
			corsProcessor.processRequest(this.config, request, response);
		}

		@Override
		@Nullable
		public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
			return this.config;
		}
	}


	private class CorsInterceptor extends HandlerInterceptorAdapter implements CorsConfigurationSource {

		@Nullable
		private final CorsConfiguration config;

		public CorsInterceptor(@Nullable CorsConfiguration config) {
			this.config = config;
		}

		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
				throws Exception {

			return corsProcessor.processRequest(this.config, request, response);
		}

		@Override
		@Nullable
		public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
			return this.config;
		}
	}

}
