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

package org.springframework.boot.actuate.security;

import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.authorization.event.AuthorizationEvent;
import org.springframework.security.authorization.event.AuthorizationGrantedEvent;

/**
 * Abstract {@link ApplicationListener} to expose Spring Security
 * {@link AuthorizationDeniedEvent authorization denied} and
 * {@link AuthorizationGrantedEvent authorization granted} events as {@link AuditEvent}s.
 *
 * @author Dave Syer
 * @author Vedran Pavic
 * @since 1.3.0
 */
public abstract class AbstractAuthorizationAuditListener
		implements ApplicationListener<AuthorizationEvent>, ApplicationEventPublisherAware {

	@SuppressWarnings("NullAway.Init")
	private ApplicationEventPublisher publisher;

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	protected ApplicationEventPublisher getPublisher() {
		return this.publisher;
	}

	protected void publish(AuditEvent event) {
		if (getPublisher() != null) {
			getPublisher().publishEvent(new AuditApplicationEvent(event));
		}
	}

}
