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

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.Registration;
import jakarta.servlet.ServletContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.Conventions;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class for Servlet 3.0+ {@link jakarta.servlet.Registration.Dynamic dynamic} based
 * registration beans.
 *
 * @param <D> the dynamic registration result
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @since 2.0.0
 */
public abstract class DynamicRegistrationBean<D extends Registration.Dynamic> extends RegistrationBean
		implements BeanNameAware {

	private static final Log logger = LogFactory.getLog(RegistrationBean.class);

	private @Nullable String name;

	private boolean asyncSupported = true;

	private Map<String, String> initParameters = new LinkedHashMap<>();

	private @Nullable String beanName;

	private boolean ignoreRegistrationFailure;

	/**
	 * Set the name of this registration. If not specified the bean name will be used.
	 * @param name the name of the registration
	 */
	public void setName(String name) {
		Assert.hasLength(name, "'name' must not be empty");
		this.name = name;
	}

	/**
	 * Sets if asynchronous operations are supported for this registration. If not
	 * specified defaults to {@code true}.
	 * @param asyncSupported if async is supported
	 */
	public void setAsyncSupported(boolean asyncSupported) {
		this.asyncSupported = asyncSupported;
	}

	/**
	 * Returns if asynchronous operations are supported for this registration.
	 * @return if async is supported
	 */
	public boolean isAsyncSupported() {
		return this.asyncSupported;
	}

	/**
	 * Set init-parameters for this registration. Calling this method will replace any
	 * existing init-parameters.
	 * @param initParameters the init parameters
	 * @see #getInitParameters
	 * @see #addInitParameter
	 */
	public void setInitParameters(Map<String, String> initParameters) {
		Assert.notNull(initParameters, "'initParameters' must not be null");
		this.initParameters = new LinkedHashMap<>(initParameters);
	}

	/**
	 * Returns a mutable Map of the registration init-parameters.
	 * @return the init parameters
	 */
	public Map<String, String> getInitParameters() {
		return this.initParameters;
	}

	/**
	 * Add a single init-parameter, replacing any existing parameter with the same name.
	 * @param name the init-parameter name
	 * @param value the init-parameter value
	 */
	public void addInitParameter(String name, String value) {
		Assert.notNull(name, "'name' must not be null");
		this.initParameters.put(name, value);
	}

	@Override
	protected final void register(String description, ServletContext servletContext) {
		D registration = addRegistration(description, servletContext);
		if (registration == null) {
			if (this.ignoreRegistrationFailure) {
				logger.info(StringUtils.capitalize(description) + " was not registered (possibly already registered?)");
				return;
			}
			throw new IllegalStateException(
					"Failed to register '%s' on the servlet context. Possibly already registered?"
						.formatted(description));
		}
		configure(registration);
	}

	/**
	 * Sets whether registration failures should be ignored. If set to true, a failure
	 * will be logged. If set to false, an {@link IllegalStateException} will be thrown.
	 * @param ignoreRegistrationFailure whether to ignore registration failures
	 * @since 3.1.0
	 */
	public void setIgnoreRegistrationFailure(boolean ignoreRegistrationFailure) {
		this.ignoreRegistrationFailure = ignoreRegistrationFailure;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	protected abstract @Nullable D addRegistration(String description, ServletContext servletContext);

	protected void configure(D registration) {
		registration.setAsyncSupported(this.asyncSupported);
		if (!this.initParameters.isEmpty()) {
			registration.setInitParameters(this.initParameters);
		}
	}

	/**
	 * Deduces the name for this registration. Will return user specified name or fallback
	 * to the bean name. If the bean name is not available, convention based naming is
	 * used.
	 * @param value the object used for convention based names
	 * @return the deduced name
	 */
	protected final String getOrDeduceName(@Nullable Object value) {
		if (this.name != null) {
			return this.name;
		}
		if (this.beanName != null) {
			return this.beanName;
		}
		if (value == null) {
			return "null";
		}
		return Conventions.getVariableName(value);
	}

}
