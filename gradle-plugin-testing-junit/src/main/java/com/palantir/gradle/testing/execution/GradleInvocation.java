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
            assertThat(result.output().contains("Configuration cache entry stored."))
                    .as("Running @WithConfigurationCache: Expected configuration cache entry to be stored, but it"
                            + " wasn't. Output: "
                            + result.output());
            assertThat(new File(gradleRunner.getProjectDir(), ".gradle/configuration-cache").exists())
                    .as("Running @WithConfigurationCache: Expected the configuration-cache directory to exits.");

            // Run a second time to verify the cache is reused
            GradleRunner secondRunner = GradleRunner.create()
                    .withProjectDir(gradleRunner.getProjectDir())
                    .withDebug(gradleRunner.isDebug())
                    .forwardOutput()
                    .withGradleVersion(gradleVersion.version())
                    .withPluginClasspath(gradleRunner.getPluginClasspath())
                    .withEnvironment(gradleRunner.getEnvironment())
                    .withArguments(gradleRunner.getArguments());

            InvocationResult secondResult = new InvocationResult(secondRunner.build());
            assertThat(secondResult.output().contains("Configuration cache entry reused."))
                    .as("Running @WithConfigurationCache: Expected configuration cache entry to be reused, but it"
                            + " wasn't. Output: "
                            + secondResult.output());

            return secondResult;
        }

        return result;
    }

    public InvocationResult buildsWithFailure() {
        InvocationResult result = new InvocationResult(gradleRunner.buildAndFail());

        if (configurationCacheEnabled) {
            assertThat(result.output().contains("Configuration cache entry stored."))
                    .as("Expected configuration cache entry to be stored, but it wasn't. Output: " + result.output());
        }

        return result;
    }
}
