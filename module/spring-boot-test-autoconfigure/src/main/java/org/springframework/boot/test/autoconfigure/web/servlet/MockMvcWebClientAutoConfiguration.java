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

package org.springframework.boot.test.autoconfigure.web.servlet;

import org.htmlunit.WebClient;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.server.test.htmlunit.LocalHostWebClient;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.htmlunit.MockMvcWebClientBuilder;

/**
 * Auto-configuration for HtmlUnit {@link WebClient} MockMVC integration.
 *
 * @author Phillip Webb
 * @since 1.4.0
 */
@AutoConfiguration(after = MockMvcAutoConfiguration.class)
@ConditionalOnClass(WebClient.class)
@ConditionalOnBooleanProperty(name = "spring.test.mockmvc.webclient.enabled", matchIfMissing = true)
public final class MockMvcWebClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean({ WebClient.class, MockMvcWebClientBuilder.class })
	@ConditionalOnBean(MockMvc.class)
	MockMvcWebClientBuilder mockMvcWebClientBuilder(MockMvc mockMvc, Environment environment) {
		return MockMvcWebClientBuilder.mockMvcSetup(mockMvc).withDelegate(new LocalHostWebClient(environment));
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(MockMvcWebClientBuilder.class)
	WebClient htmlUnitWebClient(MockMvcWebClientBuilder builder) {
		return builder.build();
	}

}
