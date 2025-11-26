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

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Extension that disables the configuration cache for tests.
 * This extension is applied via the {@link DisabledConfigurationCache} annotation.
 */
public final class DisabledConfigurationCacheExtension implements BeforeAllCallback, BeforeEachCallback {

    private static final Logger log = Logging.getLogger(DisabledConfigurationCacheExtension.class);

    @Override
    public void beforeAll(ExtensionContext context) {
        disableConfigurationCache(context);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        disableConfigurationCache(context);
    }

    private void disableConfigurationCache(ExtensionContext context) {
        log.info("Disabling configuration cache when running GradlePluginTests.");
        ConfigurationCacheStore.setConfigurationCache(context, false);
    }
}
