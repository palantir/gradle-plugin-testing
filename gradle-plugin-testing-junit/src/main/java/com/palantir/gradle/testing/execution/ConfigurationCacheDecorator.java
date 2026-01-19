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

package com.palantir.gradle.testing.execution;

import com.palantir.gradle.testing.junit.ConfigurationCacheStore;
import com.palantir.gradle.testing.junit.DecoratorContext;
import com.palantir.gradle.testing.junit.GradleInvokerDecorator;

/**
 * Decorator that enables configuration cache testing for Gradle invocations.
 *
 * <p>When applied, this decorator wraps invocations to run the build twice:
 * once to populate the configuration cache, and a second time (with --dry-run)
 * to verify the cache is properly reused.
 *
 * <p>The decorator checks {@link ConfigurationCacheStore#isConfigurationCacheEnabled} at
 * decoration time. If configuration cache has been disabled (e.g., by {@code @DisabledConfigurationCache}),
 * this decorator acts as a pass-through and returns the delegate unchanged.
 */
public final class ConfigurationCacheDecorator implements GradleInvokerDecorator {

    @Override
    public GradleInvoker decorate(DecoratorContext context, GradleInvoker delegate) {
        // Check if configuration cache is still enabled at decoration time
        // (it may have been disabled by @DisabledConfigurationCache on the method)
        if (!ConfigurationCacheStore.isConfigurationCacheEnabled(context.extensionContext())) {
            return delegate;
        }
        return new ConfigurationCacheInvoker(context.rootProjectDir(), delegate);
    }
}
