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
import java.lang.annotation.Annotation;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating {@link ConfigurationCacheDecorator} instances.
 */
public final class ConfigurationCacheDecoratorFactory implements GradleInvokerDecoratorFactory {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationCacheDecoratorFactory.class);

    @Override
    public GradleInvokerDecorator create(List<Annotation> annotations) {
        boolean disabledConfigurationCachePresent =
                annotations.stream().anyMatch(annotation -> annotation instanceof DisabledConfigurationCache);

        if (disabledConfigurationCachePresent) {
            log.debug("DisabledConfigurationCache annotation found, skipping configuration cache decoration");
            return (context, delegate) -> delegate;
        }

        return (context, delegate) -> {
            boolean configurationCacheEnabled = context.extensionContext()
                    .getConfigurationParameter("com.palantir.gradle.testing.configuration_cache_enabled")
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

            return configurationCacheEnabled ? new ConfigurationCacheDecorator().decorate(context, delegate) : delegate;
        };
    }
}
