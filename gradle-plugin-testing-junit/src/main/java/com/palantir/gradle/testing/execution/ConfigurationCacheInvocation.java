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
import java.util.Map;

public final class ConfigurationCacheInvocation implements GradleInvocation {
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
    public GradleInvocation withEnvironment(Map<String, String> environment) {
        initialGradleInvocation.withEnvironment(environment);
        secondGradleInvocation.withEnvironment(environment);
        return this;
    }

    @Override
    public InvocationResult buildsSuccessfully() {
        InvocationResult result;
        try {
            result = initialGradleInvocation.buildsSuccessfully();
            assertConfigCacheStoredOrReused(result);
            InvocationResult configurationCacheResult = secondGradleInvocation.buildsSuccessfully();
            assertConfigCacheReused(configurationCacheResult);
            return result;
        } catch (UnexpectedInvocationFailure unexpectedBuildFailure) {
            if (unexpectedBuildFailure.getMessage().contains("Configuration cache problems found in this build")) {
                throw new UnexpectedConfigurationCacheFailure(
                        String.format("""
                            Unexpected build execution failure. Build Execution failure caused by configuration cache incompatibility.

                            Output:
                            %s
                            """, unexpectedBuildFailure.getResult().output()),
                        unexpectedBuildFailure.getResult());
            }
            throw unexpectedBuildFailure;
        }
    }

    @Override
    public InvocationResult buildsWithFailure() {
        InvocationResult result = initialGradleInvocation.buildsWithFailure();
        assertConfigCacheStoredOrReused(result);
        return result;
    }

    private void assertConfigCacheStoredOrReused(InvocationResult result) {
        if (!result.output().contains("Configuration cache entry stored.")
                // We might be still re-using the cache from a previous call.
                && !result.output().contains("Configuration cache entry reused.")) {
            throw new UnexpectedConfigurationCacheFailure(String.format("""
                The GradleInvocation was run with configuration cache enabled. Expected configuration cache entry to be stored, but it wasn't.
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
                        The GradleInvocation was run with configuration cache enabled. Expected the configuration-cache %s directory to exist, but it doesn't exist.
                        Check the output for any configuration cache problems.

                        Output:
                        %s
                        """, configurationCacheDir, result.output()), result);
        }
    }

    private void assertConfigCacheReused(InvocationResult result) {
        if (!result.output().contains("Configuration cache entry reused.")) {
            throw new UnexpectedConfigurationCacheFailure(String.format("""
                The GradleInvocation was run with configuration cache enabled.
                When configuration cache is enabled during tests there will be 2 gradle invocations:
                    - the first one that runs the tasks and checks that the configuration cache was stored. In this case the first invocation was successful.
                    - the second one that runs the same tasks with `--dry-run` and checks that the configuration cache stored previously was loaded. In this case, this invocation failed.
                Check the output for any configuration cache problems.

                Output:
                %s
                """, result.output()), result);
        }
    }
}
