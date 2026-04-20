/*
 * (c) Copyright 2024 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.gradle.plugintesting;

import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

public abstract class PluginTestingExtension {
    public static final String EXTENSION_NAME = "gradleTestUtils";

    /**
     * Whether to set the ignoreDeprecations system property when running tests.  This is for nebula tests that will
     * fail if there are gradle deprecations.
     */
    public abstract Property<Boolean> getIgnoreGradleDeprecations();

    /**
     * Gradle versions to test against.
     * @deprecated Use {@code gradle/gradle-test-versions.yml} to set Gradle versions to test against. This property
     * will be removed as part of rolling out {@code gradle/gradle-test-versions.yml}.
     */
    @Deprecated
    public abstract SetProperty<String> getGradleVersions();

    /**
     * Whether the configuration cache is enabled for tests.
     */
    public abstract Property<Boolean> getConfigurationCacheEnabled();

    /**
     * Base URL for downloading Gradle distributions. When set, tests will download Gradle from
     * {@code {baseUrl}/gradle-{version}-bin.zip} instead of the default Gradle distribution server.
     */
    public abstract Property<String> getGradleDistributionBaseUrl();

    public PluginTestingExtension() {
        getIgnoreGradleDeprecations().convention(true);
        getConfigurationCacheEnabled().convention(false);
    }
}
