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

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Extension that handles the configuration cache setting.
 * This is used by the {@link GradlePluginTests} annotation to set up the default configuration cache behavior.
 */
public final class ConfigurationCacheParameterExtension implements BeforeAllCallback, BeforeEachCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        setConfigurationCache(context);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        setConfigurationCache(context);
    }

    private void setConfigurationCache(ExtensionContext context) {
        Boolean configurationCacheEnabled = context.getConfigurationParameter(
                        "com.palantir.gradle.testing.configuration_cache_enabled")
                .map(Boolean::parseBoolean)
                .orElseThrow(() -> new RuntimeException(
                        "Could not configure whether to run the tests with configuration-cache.Have you"
                                + " applied the latest `com.palantir.gradle-plugin-testing` plugin to this"
                                + " project?"));
        ConfigurationCacheStore.setConfigurationCache(context, configurationCacheEnabled);
    }
}
