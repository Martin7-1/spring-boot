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

package org.springframework.boot.docker.compose.lifecycle;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.docker.compose.core.DockerCompose;

/**
 * Command used to stop Docker Compose.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public enum StopCommand {

	/**
	 * Stop using {@code docker compose down}.
	 */
	DOWN(DockerCompose::down),

	/**
	 * Stop using {@code docker compose stop}.
	 */
	STOP(DockerCompose::stop);

	private final Command command;

	StopCommand(Command command) {
		this.command = command;
	}

	void applyTo(DockerCompose dockerCompose, Duration timeout, List<String> arguments) {
		this.command.applyTo(dockerCompose, timeout, arguments);
	}

	@FunctionalInterface
	private interface Command {

		void applyTo(DockerCompose dockerCompose, Duration timeout, List<String> arguments);

	}

}
