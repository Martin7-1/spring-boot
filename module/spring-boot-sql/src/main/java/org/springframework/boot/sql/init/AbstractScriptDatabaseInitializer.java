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

package org.springframework.boot.sql.init;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.CollectionUtils;

/**
 * Base class for an {@link InitializingBean} that performs SQL database initialization
 * using schema (DDL) and data (DML) scripts.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 */
public abstract class AbstractScriptDatabaseInitializer implements ResourceLoaderAware, InitializingBean {

	private static final String OPTIONAL_LOCATION_PREFIX = "optional:";

	private final DatabaseInitializationSettings settings;

	@SuppressWarnings("NullAway.Init")
	private volatile ResourceLoader resourceLoader;

	/**
	 * Creates a new {@link AbstractScriptDatabaseInitializer} that will initialize the
	 * database using the given settings.
	 * @param settings initialization settings
	 */
	protected AbstractScriptDatabaseInitializer(DatabaseInitializationSettings settings) {
		this.settings = settings;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		initializeDatabase();
	}

	/**
	 * Initializes the database by applying schema and data scripts.
	 * @return {@code true} if one or more scripts were applied to the database, otherwise
	 * {@code false}
	 */
	public boolean initializeDatabase() {
		ScriptLocationResolver locationResolver = new ScriptLocationResolver(this.resourceLoader);
		boolean initialized = applySchemaScripts(locationResolver);
		return applyDataScripts(locationResolver) || initialized;
	}

	private boolean isEnabled() {
		if (this.settings.getMode() == DatabaseInitializationMode.NEVER) {
			return false;
		}
		return this.settings.getMode() == DatabaseInitializationMode.ALWAYS || isEmbeddedDatabase();
	}

	/**
	 * Returns whether the database that is to be initialized is embedded.
	 * @return {@code true} if the database is embedded, otherwise {@code false}
	 * @since 2.5.1
	 */
	protected boolean isEmbeddedDatabase() {
		throw new IllegalStateException(
				"Database initialization mode is '" + this.settings.getMode() + "' and database type is unknown");
	}

	private boolean applySchemaScripts(ScriptLocationResolver locationResolver) {
		return applyScripts(this.settings.getSchemaLocations(), "schema", locationResolver);
	}

	private boolean applyDataScripts(ScriptLocationResolver locationResolver) {
		return applyScripts(this.settings.getDataLocations(), "data", locationResolver);
	}

	private boolean applyScripts(@Nullable List<String> locations, String type,
			ScriptLocationResolver locationResolver) {
		List<Resource> scripts = getScripts(locations, type, locationResolver);
		if (!scripts.isEmpty() && isEnabled()) {
			runScripts(scripts);
			return true;
		}
		return false;
	}

	private List<Resource> getScripts(@Nullable List<String> locations, String type,
			ScriptLocationResolver locationResolver) {
		if (CollectionUtils.isEmpty(locations)) {
			return Collections.emptyList();
		}
		List<Resource> resources = new ArrayList<>();
		for (String location : locations) {
			boolean optional = location.startsWith(OPTIONAL_LOCATION_PREFIX);
			if (optional) {
				location = location.substring(OPTIONAL_LOCATION_PREFIX.length());
			}
			for (Resource resource : doGetResources(location, locationResolver)) {
				if (resource.isReadable()) {
					resources.add(resource);
				}
				else if (!optional) {
					throw new IllegalStateException("No " + type + " scripts found at location '" + location + "'");
				}
			}
		}
		return resources;
	}

	private List<Resource> doGetResources(String location, ScriptLocationResolver locationResolver) {
		try {
			return locationResolver.resolve(location);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to load resources from " + location, ex);
		}
	}

	private void runScripts(List<Resource> resources) {
		runScripts(new Scripts(resources).continueOnError(this.settings.isContinueOnError())
			.separator(this.settings.getSeparator())
			.encoding(this.settings.getEncoding()));
	}

	/**
	 * Initialize the database by running the given {@code scripts}.
	 * @param scripts the scripts to run
	 * @since 3.0.0
	 */
	protected abstract void runScripts(Scripts scripts);

	private static class ScriptLocationResolver {

		private final ResourcePatternResolver resourcePatternResolver;

		ScriptLocationResolver(ResourceLoader resourceLoader) {
			this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
		}

		private List<Resource> resolve(String location) throws IOException {
			List<Resource> resources = new ArrayList<>(
					Arrays.asList(this.resourcePatternResolver.getResources(location)));
			resources.sort((r1, r2) -> {
				try {
					return r1.getURL().toString().compareTo(r2.getURL().toString());
				}
				catch (IOException ex) {
					return 0;
				}
			});
			return resources;
		}

	}

	/**
	 * Scripts to be used to initialize the database.
	 *
	 * @since 3.0.0
	 */
	public static class Scripts implements Iterable<Resource> {

		private final List<Resource> resources;

		private boolean continueOnError;

		private String separator = ";";

		private @Nullable Charset encoding;

		public Scripts(List<Resource> resources) {
			this.resources = resources;
		}

		@Override
		public Iterator<Resource> iterator() {
			return this.resources.iterator();
		}

		public Scripts continueOnError(boolean continueOnError) {
			this.continueOnError = continueOnError;
			return this;
		}

		public boolean isContinueOnError() {
			return this.continueOnError;
		}

		public Scripts separator(String separator) {
			this.separator = separator;
			return this;
		}

		public String getSeparator() {
			return this.separator;
		}

		public Scripts encoding(@Nullable Charset encoding) {
			this.encoding = encoding;
			return this;
		}

		public @Nullable Charset getEncoding() {
			return this.encoding;
		}

	}

}
