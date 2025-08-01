/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.web.servlet;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * A {@link ServletContextInitializer} to register {@link Servlet}s in a Servlet 3.0+
 * container. Similar to the {@link ServletContext#addServlet(String, Servlet)
 * registration} features provided by {@link ServletContext} but with a Spring Bean
 * friendly design.
 * <p>
 * The {@link #setServlet(Servlet) servlet} must be specified before calling
 * {@link #onStartup}. URL mapping can be configured used {@link #setUrlMappings} or
 * omitted when mapping to '/*' (unless
 * {@link #ServletRegistrationBean(Servlet, boolean, String...) alwaysMapUrl} is set to
 * {@code false}). The servlet name will be deduced if not specified.
 *
 * @param <T> the type of the {@link Servlet} to register
 * @author Phillip Webb
 * @since 1.4.0
 * @see ServletContextInitializer
 * @see ServletContext#addServlet(String, Servlet)
 * @see org.springframework.boot.web.servlet.ServletRegistration
 */
public class ServletRegistrationBean<T extends Servlet> extends DynamicRegistrationBean<ServletRegistration.Dynamic> {

	private static final String[] DEFAULT_MAPPINGS = { "/*" };

	private @Nullable T servlet;

	private Set<String> urlMappings = new LinkedHashSet<>();

	private boolean alwaysMapUrl = true;

	private int loadOnStartup = -1;

	private @Nullable MultipartConfigElement multipartConfig;

	/**
	 * Create a new {@link ServletRegistrationBean} instance.
	 */
	public ServletRegistrationBean() {
	}

	/**
	 * Create a new {@link ServletRegistrationBean} instance with the specified
	 * {@link Servlet} and URL mappings.
	 * @param servlet the servlet being mapped
	 * @param urlMappings the URLs being mapped
	 */
	public ServletRegistrationBean(T servlet, String... urlMappings) {
		this(servlet, true, urlMappings);
	}

	/**
	 * Create a new {@link ServletRegistrationBean} instance with the specified
	 * {@link Servlet} and URL mappings.
	 * @param servlet the servlet being mapped
	 * @param alwaysMapUrl if omitted URL mappings should be replaced with '/*'
	 * @param urlMappings the URLs being mapped
	 */
	public ServletRegistrationBean(T servlet, boolean alwaysMapUrl, String... urlMappings) {
		Assert.notNull(servlet, "'servlet' must not be null");
		Assert.notNull(urlMappings, "'urlMappings' must not be null");
		this.servlet = servlet;
		this.alwaysMapUrl = alwaysMapUrl;
		this.urlMappings.addAll(Arrays.asList(urlMappings));
	}

	/**
	 * Sets the servlet to be registered.
	 * @param servlet the servlet
	 */
	public void setServlet(T servlet) {
		Assert.notNull(servlet, "'servlet' must not be null");
		this.servlet = servlet;
	}

	/**
	 * Return the servlet being registered.
	 * @return the servlet
	 */
	public @Nullable T getServlet() {
		return this.servlet;
	}

	/**
	 * Set the URL mappings for the servlet. If not specified the mapping will default to
	 * '/'. This will replace any previously specified mappings.
	 * @param urlMappings the mappings to set
	 * @see #addUrlMappings(String...)
	 */
	public void setUrlMappings(Collection<String> urlMappings) {
		Assert.notNull(urlMappings, "'urlMappings' must not be null");
		this.urlMappings = new LinkedHashSet<>(urlMappings);
	}

	/**
	 * Return a mutable collection of the URL mappings, as defined in the Servlet
	 * specification, for the servlet.
	 * @return the urlMappings
	 */
	public Collection<String> getUrlMappings() {
		return this.urlMappings;
	}

	/**
	 * Add URL mappings, as defined in the Servlet specification, for the servlet.
	 * @param urlMappings the mappings to add
	 * @see #setUrlMappings(Collection)
	 */
	public void addUrlMappings(String... urlMappings) {
		Assert.notNull(urlMappings, "'urlMappings' must not be null");
		this.urlMappings.addAll(Arrays.asList(urlMappings));
	}

	/**
	 * Sets the {@code loadOnStartup} priority. See
	 * {@link ServletRegistration.Dynamic#setLoadOnStartup} for details.
	 * @param loadOnStartup if load on startup is enabled
	 */
	public void setLoadOnStartup(int loadOnStartup) {
		this.loadOnStartup = loadOnStartup;
	}

	/**
	 * Set the {@link MultipartConfigElement multi-part configuration}.
	 * @param multipartConfig the multipart configuration to set or {@code null}
	 */
	public void setMultipartConfig(@Nullable MultipartConfigElement multipartConfig) {
		this.multipartConfig = multipartConfig;
	}

	/**
	 * Returns the {@link MultipartConfigElement multi-part configuration} to be applied
	 * or {@code null}.
	 * @return the multipart config
	 */
	public @Nullable MultipartConfigElement getMultipartConfig() {
		return this.multipartConfig;
	}

	@Override
	protected String getDescription() {
		Assert.state(this.servlet != null, "Unable to return description for null servlet");
		return "servlet " + getServletName();
	}

	@Override
	protected ServletRegistration.Dynamic addRegistration(String description, ServletContext servletContext) {
		String name = getServletName();
		return servletContext.addServlet(name, this.servlet);
	}

	/**
	 * Configure registration settings. Subclasses can override this method to perform
	 * additional configuration if required.
	 * @param registration the registration
	 */
	@Override
	protected void configure(ServletRegistration.Dynamic registration) {
		super.configure(registration);
		String[] urlMapping = StringUtils.toStringArray(this.urlMappings);
		if (urlMapping.length == 0 && this.alwaysMapUrl) {
			urlMapping = DEFAULT_MAPPINGS;
		}
		if (!ObjectUtils.isEmpty(urlMapping)) {
			registration.addMapping(urlMapping);
		}
		registration.setLoadOnStartup(this.loadOnStartup);
		if (this.multipartConfig != null) {
			registration.setMultipartConfig(this.multipartConfig);
		}
	}

	/**
	 * Returns the servlet name that will be registered.
	 * @return the servlet name
	 */
	public String getServletName() {
		return getOrDeduceName(this.servlet);
	}

	@Override
	public String toString() {
		return getServletName() + " urls=" + getUrlMappings();
	}

}
