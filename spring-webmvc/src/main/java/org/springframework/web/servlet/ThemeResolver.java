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

package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;

/**
 * 基于web的主题解析策略的接口，允许通过请求进行主题解析，也允许通过请求和响应进行主题修改。
 *
 * <p>这个接口允许基于会话、cookie等的实现。默认实现是
 * {@link org.springframework.web.servlet.theme.FixedThemeResolver}，只是使用配置的默认主题。
 *
 * <p>请注意，此解析器仅负责确定当前主题名称。
 * 解析后的主题名的主题实例由DispatcherServlet通过各自的主题资源(即当前的WebApplicationContext)来查找。
 *
 * <p>使用{@link org.springframework.web.servlet.support.RequestContext#getTheme()}
 * 来检索控制器或视图中的当前主题，独立于实际的解析策略。
 *
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 * @since 17.06.2003
 * @see org.springframework.ui.context.Theme
 * @see org.springframework.ui.context.ThemeSource
 */
public interface ThemeResolver {

	/**
	 * Resolve the current theme name via the given request.
	 * Should return a default theme as fallback in any case.
	 * @param request request to be used for resolution
	 * @return the current theme name
	 */
	String resolveThemeName(HttpServletRequest request);

	/**
	 * Set the current theme name to the given one.
	 * @param request request to be used for theme name modification
	 * @param response response to be used for theme name modification
	 * @param themeName the new theme name ({@code null} or empty to reset it)
	 * @throws UnsupportedOperationException if the ThemeResolver implementation
	 * does not support dynamic changing of the theme
	 */
	void setThemeName(HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable String themeName);

}
