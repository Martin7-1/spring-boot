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

package org.springframework.boot.data.neo4j.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring Data Neo4j.
 *
 * @author Michael J. Simons
 * @since 4.0.0
 */
@ConfigurationProperties("spring.data.neo4j")
public class Neo4jDataProperties {

	/**
	 * Database name to use. By default, the server decides the default database to use.
	 */
	private @Nullable String database;

	public @Nullable String getDatabase() {
		return this.database;
	}

	public void setDatabase(@Nullable String database) {
		this.database = database;
	}

}
