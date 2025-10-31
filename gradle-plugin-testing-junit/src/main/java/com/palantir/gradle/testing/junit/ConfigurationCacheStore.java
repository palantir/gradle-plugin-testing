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

import org.assertj.core.util.VisibleForTesting;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

/**
 * Store for configuration cache state in JUnit extension context.
 */
@VisibleForTesting
final class ConfigurationCacheStore {
    private static final Namespace NAMESPACE = Namespace.create(ConfigurationCacheStore.class);
    private static final String CONFIG_CACHE_ENABLED = "configCacheEnabled";

    /**
     * Returns whether configuration cache is enabled for the given extension context.
     */
    public static boolean isConfigurationCacheEnabled(ExtensionContext context) {
        return Boolean.TRUE.equals(context.getStore(NAMESPACE).get(CONFIG_CACHE_ENABLED, Boolean.class));
    }

    /**
     * Sets configuration cache enabled/disabled for the given extension context.
     */
    public static void setConfigurationCache(ExtensionContext context, boolean isEnabled) {
        context.getStore(NAMESPACE).put(CONFIG_CACHE_ENABLED, isEnabled);
    }

    private ConfigurationCacheStore() {}
}
