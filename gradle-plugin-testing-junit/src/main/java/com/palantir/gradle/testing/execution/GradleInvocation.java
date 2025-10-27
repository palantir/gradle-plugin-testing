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

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.Map;
import org.gradle.testkit.runner.GradleRunner;

public final class GradleInvocation {
    private final GradleRunner gradleRunner;
    private final GradleVersion gradleVersion;
    private final boolean configurationCacheEnabled;

    GradleInvocation(GradleRunner gradleRunner, GradleVersion gradleVersion, boolean configurationCacheEnabled) {
        this.gradleRunner = gradleRunner;
        this.gradleVersion = gradleVersion;
        this.configurationCacheEnabled = configurationCacheEnabled;
    }

    public GradleInvocation withEnvironment(Map<String, String> environment) {
        gradleRunner.withEnvironment(environment);
        return this;
    }

    public InvocationResult buildsSuccessfully() {
        InvocationResult result = new InvocationResult(gradleRunner.build());

        if (configurationCacheEnabled) {
            assertConfigCacheStored(result.output());

            // Run a second time to verify the cache is reused, we don't need to run the tasks --dry-run is enough to
            // check if the configuration cache was used.
            GradleRunner configurationCacheRunner = GradleRunner.create()
                    .withProjectDir(gradleRunner.getProjectDir())
                    .withDebug(gradleRunner.isDebug())
                    .forwardOutput()
                    .withGradleVersion(gradleVersion.version())
                    .withPluginClasspath(gradleRunner.getPluginClasspath())
                    .withEnvironment(gradleRunner.getEnvironment())
                    .withArguments(new ImmutableList.Builder<String>()
                            .addAll(gradleRunner.getArguments())
                            .add("--dry-run")
                            .build());
            InvocationResult configurationCacheResult = new InvocationResult(configurationCacheRunner.build());
            assertConfigCacheReused(configurationCacheResult.output());
        }

        return result;
    }

    public InvocationResult buildsWithFailure() {
        InvocationResult result = new InvocationResult(gradleRunner.buildAndFail());

        if (configurationCacheEnabled) {
            assertConfigCacheStored(result.output());
        }

        return result;
    }

    private void assertConfigCacheStored(String output) {
        assertThat(output.contains("Configuration cache entry stored."))
                .as(String.format(
                        "Running @WithConfigurationCache: Expected configuration cache entry to be stored, but it"
                                + " wasn't. Output: %s",
                        output));

        File configurationCacheDir = new File(gradleRunner.getProjectDir(), ".gradle/configuration-cache");
        assertThat(configurationCacheDir.exists())
                .as(String.format(
                        "Running @WithConfigurationCache: Expected the configuration-cache %s directory to exits.",
                        configurationCacheDir));
    }

    private void assertConfigCacheReused(String output) {
        assertThat(output.contains("Configuration cache entry reused."))
                .as(String.format(
                        "Running @WithConfigurationCache: Expected configuration cache entry to be reused, but it"
                                + " wasn't. Output: %s",
                        output));
    }
}
