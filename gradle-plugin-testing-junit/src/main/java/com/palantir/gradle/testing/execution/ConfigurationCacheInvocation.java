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

import java.io.File;
import java.nio.file.Path;
import org.gradle.testkit.runner.UnexpectedBuildFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfigurationCacheInvocation implements GradleInvocation {
    private static final Logger log = LoggerFactory.getLogger(ConfigurationCacheInvocation.class);
    private final Path projectDir;
    private final GradleInvocation initialGradleInvocation;
    private final GradleInvocation secondGradleInvocation;

    public ConfigurationCacheInvocation(
            Path projectDir, GradleInvocation initialGradleInvocation, GradleInvocation secondGradleInvocation) {
        this.projectDir = projectDir;
        this.initialGradleInvocation = initialGradleInvocation;
        this.secondGradleInvocation = secondGradleInvocation;
    }

    @Override
    public InvocationResult buildsSuccessfully() {
        InvocationResult result;
        try {
            result = initialGradleInvocation.buildsSuccessfully();
            assertConfigCacheStored(result);
            InvocationResult configurationCacheResult = secondGradleInvocation.buildsSuccessfully();
            assertConfigCacheReused(configurationCacheResult);
            return result;
        } catch (UnexpectedBuildFailure unexpectedBuildFailure) {
            if (unexpectedBuildFailure.getMessage().contains("Configuration cache problems found in this build")) {
                throw new UnexpectedConfigurationCacheFailure(
                        String.format(
                                """
                                Configuration cache incompatibility: Build execution failed.

                                This test runs with configuration cache enabled (`gradleTestUtils.configurationCacheEnabled=true`).
                                Please check the output below for specific configuration cache problems.

                                Output:
                                %s
                                """, unexpectedBuildFailure.getBuildResult().getOutput()),
                        new InvocationResult(unexpectedBuildFailure.getBuildResult()));
            }
            throw unexpectedBuildFailure;
        }
    }

    @Override
    public InvocationResult buildsWithFailure() {
        InvocationResult result = initialGradleInvocation.buildsWithFailure();
        assertConfigCacheStored(result);
        InvocationResult configurationCacheResult = secondGradleInvocation.buildsSuccessfully();
        assertConfigCacheReused(configurationCacheResult);
        return result;
    }

    private void assertConfigCacheStored(InvocationResult result) {
        if (!result.output().contains("Configuration cache entry stored.")) {
            throw new UnexpectedConfigurationCacheFailure(String.format("""
                Build Execution failure caused by configuration cache issues. Expected configuration cache entry to be stored, but it wasn't.
                The GradleInvocation was run with configuration cache enabled because the `gradleTestUtils.configurationCacheEnabled` property was set.
                Check the output for configuration cache problems.

                Output:
                %s
                """, result.output()), result);
        }
        File configurationCacheDir =
                projectDir.resolve(".gradle/configuration-cache").toFile();
        if (!configurationCacheDir.exists()) {
            throw new UnexpectedConfigurationCacheFailure(
                    String.format("""
                        Configuration cache directory missing: Expected directory `%s` not found.

                        This test runs with configuration cache enabled (`gradleTestUtils.configurationCacheEnabled=true`).
                        Please check the output below for specific configuration cache problems.

                        Output:
                        %s
                        """, configurationCacheDir, result.output()), result);
        }
    }

    private void assertConfigCacheReused(InvocationResult result) {
        if (!result.output().contains("Configuration cache entry reused.")) {
            throw new UnexpectedConfigurationCacheFailure(String.format("""
                Configuration cache reuse failure: The second run failed to reuse the cached configuration.

                This test runs with configuration cache enabled (`gradleTestUtils.configurationCacheEnabled=true`).
                Test sequence:
                    ✓ First run: Successfully executed tasks and stored configuration cache
                    ✗ Second run: Failed to reuse configuration cache during dry-run verification

                Please check the output below for specific configuration cache problems.

                Output:
                %s
                """, result.output()), result);
        }
    }
}
