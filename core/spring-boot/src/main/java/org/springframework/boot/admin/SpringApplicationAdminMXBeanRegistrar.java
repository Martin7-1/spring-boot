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

package org.springframework.boot.admin;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.Assert;

/**
 * Register a {@link SpringApplicationAdminMXBean} implementation to the platform
 * {@link MBeanServer}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 1.3.0
 */
public class SpringApplicationAdminMXBeanRegistrar implements ApplicationContextAware, GenericApplicationListener,
		EnvironmentAware, InitializingBean, DisposableBean {

	private static final Log logger = LogFactory.getLog(SpringApplicationAdmin.class);

	@SuppressWarnings("NullAway.Init")
	private ConfigurableApplicationContext applicationContext;

	private Environment environment = new StandardEnvironment();

	private final ObjectName objectName;

	private boolean ready;

	public SpringApplicationAdminMXBeanRegistrar(String name) throws MalformedObjectNameException {
		this.objectName = new ObjectName(name);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Assert.state(applicationContext instanceof ConfigurableApplicationContext,
				"'applicationContext' must be a ConfigurableApplicationContext");
		this.applicationContext = (ConfigurableApplicationContext) applicationContext;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public boolean supportsEventType(ResolvableType eventType) {
		Class<?> type = eventType.getRawClass();
		if (type == null) {
			return false;
		}
		return ApplicationReadyEvent.class.isAssignableFrom(type);
	}

	@Override
	public boolean supportsSourceType(@Nullable Class<?> sourceType) {
		return true;
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ApplicationReadyEvent readyEvent) {
			onApplicationReadyEvent(readyEvent);
		}
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	void onApplicationReadyEvent(ApplicationReadyEvent event) {
		if (this.applicationContext.equals(event.getApplicationContext())) {
			this.ready = true;
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		server.registerMBean(new SpringApplicationAdmin(), this.objectName);
		if (logger.isDebugEnabled()) {
			logger.debug("Application Admin MBean registered with name '" + this.objectName + "'");
		}
	}

	@Override
	public void destroy() throws Exception {
		ManagementFactory.getPlatformMBeanServer().unregisterMBean(this.objectName);
	}

	private final class SpringApplicationAdmin implements SpringApplicationAdminMXBean {

		@Override
		public boolean isReady() {
			return SpringApplicationAdminMXBeanRegistrar.this.ready;
		}

		@Override
		public boolean isEmbeddedWebApplication() {
			return SpringApplicationAdminMXBeanRegistrar.this.environment instanceof ConfigurableEnvironment configurableEnvironment
					&& configurableEnvironment.getPropertySources().get("server.ports") != null;
		}

		@Override
		public @Nullable String getProperty(String key) {
			return SpringApplicationAdminMXBeanRegistrar.this.environment.getProperty(key);
		}

		@Override
		public void shutdown() {
			logger.info("Application shutdown requested.");
			SpringApplicationAdminMXBeanRegistrar.this.applicationContext.close();
		}

	}

}
