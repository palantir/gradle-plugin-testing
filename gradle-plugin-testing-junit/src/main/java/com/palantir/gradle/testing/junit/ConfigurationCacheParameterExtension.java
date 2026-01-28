/*
 * (c) Copyright 2025 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.gradle.testing.junit;

import com.palantir.gradle.testing.execution.ConfigurationCacheDecorator;
import com.palantir.gradle.testing.execution.GradleInvoker;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension that handles the configuration cache setting.
 * This is used by the {@link GradlePluginTests} annotation to set up the default configuration cache behavior.
 *
 * <p>When configuration cache is enabled (and not disabled by {@link DisabledConfigurationCache}),
 * this extension registers a {@link ConfigurationCacheDecorator} that wraps Gradle invocations
 * to test configuration cache compatibility.
 */
public final class ConfigurationCacheParameterExtension implements BeforeAllCallback, BeforeEachCallback {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationCacheParameterExtension.class);

    @Override
    public void beforeAll(ExtensionContext context) {
        maybeRegisterConfigurationCacheDecorator(context);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        maybeRegisterConfigurationCacheDecorator(context);
    }

    private void maybeRegisterConfigurationCacheDecorator(ExtensionContext context) {
        // Check if already processed (either enabled or explicitly disabled)
        if (ConfigurationCacheStore.hasConfigurationCacheValue(context)) {
            log.debug(
                    "Configuration Cache value is already set by an extension to value = {}, not overriding it.",
                    ConfigurationCacheStore.isConfigurationCacheEnabled(context));
            return;
        }

        boolean configurationCacheEnabled = context.getConfigurationParameter(
                        "com.palantir.gradle.testing.configuration_cache_enabled")
                .map(Boolean::parseBoolean)
                .orElseThrow(() -> new RuntimeException(
                        "Could not configure whether to run the tests with configuration-cache. Have you"
                                + " applied the latest `com.palantir.gradle-plugin-testing` plugin to this"
                                + " project?"));

        if (GradleInvoker.shouldRunInTestkitDebugMode()) {
            log.warn("Configuration cache disabled because debug mode is active. Debug mode and"
                    + " configuration cache cannot be used together. See"
                    + " https://github.com/gradle/gradle/issues/25846 for details.");
            configurationCacheEnabled = false;
        }

        // Store the value so other extensions can check it
        ConfigurationCacheStore.setConfigurationCache(context, configurationCacheEnabled);
    }
}
