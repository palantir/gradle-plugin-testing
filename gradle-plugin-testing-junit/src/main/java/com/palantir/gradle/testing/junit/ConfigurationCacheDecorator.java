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

import com.palantir.gradle.testing.execution.ConfigurationCacheInvoker;
import com.palantir.gradle.testing.execution.GradleInvoker;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfigurationCacheDecorator implements GradleInvokerDecorator<DisabledConfigurationCache> {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationCacheDecorator.class);

    @Override
    public GradleInvoker decorate(
            DecoratorContext context, GradleInvoker delegate, List<DisabledConfigurationCache> annotations) {

        if (!annotations.isEmpty()) {
            log.debug("DisabledConfigurationCache annotation found, skipping configuration cache decoration");
            return delegate;
        }

        boolean configurationCacheEnabled = context.extensionContext()
                .getConfigurationParameter("com.palantir.gradle.testing.configuration_cache_enabled")
                .map(Boolean::parseBoolean)
                .orElseThrow(() -> new RuntimeException(
                        "Could not configure whether to run the tests with configuration-cache. Have you"
                                + " applied the latest `com.palantir.gradle-plugin-testing` plugin to this"
                                + " project?"));
        if (!configurationCacheEnabled) {
            return delegate;
        }

        if (GradleInvoker.shouldRunInTestkitDebugMode()) {
            log.warn("Configuration cache disabled because debug mode is active. Debug mode and"
                    + " configuration cache cannot be used together. See"
                    + " https://github.com/gradle/gradle/issues/25846 for details.");
            return delegate;
        }

        return new ConfigurationCacheInvoker(context.rootProject().path(), delegate);
    }
}
