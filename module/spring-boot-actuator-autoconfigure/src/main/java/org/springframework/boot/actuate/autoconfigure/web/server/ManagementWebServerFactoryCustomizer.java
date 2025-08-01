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

package org.springframework.boot.actuate.autoconfigure.web.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

/**
 * {@link WebServerFactoryCustomizer} that customizes the {@link WebServerFactory} used to
 * create the management context's web server.
 *
 * @param <T> the type of web server factory to customize
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class ManagementWebServerFactoryCustomizer<T extends ConfigurableWebServerFactory>
		implements WebServerFactoryCustomizer<T>, Ordered {

	private final ListableBeanFactory beanFactory;

	private final Class<? extends WebServerFactoryCustomizer<?>> @Nullable [] customizerClasses;

	/**
	 * Creates a new customizer that will retrieve beans using the given
	 * {@code beanFactory}.
	 * @param beanFactory the bean factory to use
	 * @since 3.5.0
	 */
	public ManagementWebServerFactoryCustomizer(ListableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		this.customizerClasses = null;
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public final void customize(T factory) {
		ManagementServerProperties managementServerProperties = BeanFactoryUtils
			.beanOfTypeIncludingAncestors(this.beanFactory, ManagementServerProperties.class);
		// Customize as per the parent context first (so e.g. the access logs go to
		// the same place)
		if (this.customizerClasses != null) {
			customizeSameAsParentContext(factory);
		}
		// Then reset the error pages
		factory.setErrorPages(Collections.emptySet());
		// and add the management-specific bits
		ServerProperties serverProperties = BeanFactoryUtils.beanOfTypeIncludingAncestors(this.beanFactory,
				ServerProperties.class);
		customize(factory, managementServerProperties, serverProperties);
	}

	private void customizeSameAsParentContext(T factory) {
		List<WebServerFactoryCustomizer<?>> customizers = new ArrayList<>();
		Assert.state(this.customizerClasses != null, "'customizerClasses' must not be null");
		for (Class<? extends WebServerFactoryCustomizer<?>> customizerClass : this.customizerClasses) {
			try {
				customizers.add(BeanFactoryUtils.beanOfTypeIncludingAncestors(this.beanFactory, customizerClass));
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore
			}
		}
		invokeCustomizers(factory, customizers);
	}

	@SuppressWarnings("unchecked")
	private void invokeCustomizers(T factory, List<WebServerFactoryCustomizer<?>> customizers) {
		LambdaSafe.callbacks(WebServerFactoryCustomizer.class, customizers, factory)
			.invoke((customizer) -> customizer.customize(factory));
	}

	protected void customize(T factory, ManagementServerProperties managementServerProperties,
			ServerProperties serverProperties) {
		Integer port = managementServerProperties.getPort();
		Assert.state(port != null, "'port' must not be null");
		factory.setPort(port);
		Ssl ssl = managementServerProperties.getSsl();
		if (ssl != null) {
			factory.setSsl(ssl);
		}
		factory.setServerHeader(serverProperties.getServerHeader());
		factory.setAddress(managementServerProperties.getAddress());
	}

}
