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

package com.palantir.gradle.plugintesting;

import static com.palantir.gradle.testing.assertion.GradlePluginTestAssertions.assertThat;

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.execution.InvocationResult;
import com.palantir.gradle.testing.project.RootProject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;

/**
 * Abstract base class providing helper methods for testing Gradle plugins with configuration cache.
 *
 * <p>This class provides utilities for manual configuration cache testing scenarios that require
 * explicit control over cache storage and reuse, including cleanup between runs.
 *
 * <p>For standard configuration cache testing, the framework's automatic configuration cache support
 * should be used instead (enabled by default). Use {@link com.palantir.gradle.testing.junit.DisabledConfigurationCache}
 * annotation to disable automatic configuration cache testing when needed.
 */
public abstract class ConfigurationCacheTest {

    /**
     * Runs tasks with configuration cache, verifying that the cache is stored on first run
     * and reused on second run.
     *
     * @param gradle the Gradle invoker
     * @param rootProject the root project
     * @param tasks the tasks to execute
     * @return the result of the first run
     */
    protected InvocationResult runTasksWithConfigurationCacheAndCheck(
            GradleInvoker gradle, RootProject rootProject, String... tasks) {
        InvocationResult firstRun = runTasksWithConfigurationCache(gradle, rootProject, false, false, tasks);
        assertThat(firstRun).output().contains("Configuration cache entry stored.");

        InvocationResult secondRun = runTasksWithConfigurationCache(gradle, rootProject, true, false, tasks);
        assertThat(secondRun).output().contains("Configuration cache entry reused.");

        return firstRun;
    }

    /**
     * Runs tasks with configuration cache expecting a failure, verifying that the cache is stored.
     *
     * @param gradle the Gradle invoker
     * @param rootProject the root project
     * @param tasks the tasks to execute
     * @return the result of the run
     */
    protected InvocationResult runTasksAndFailWithConfigurationCache(
            GradleInvoker gradle, RootProject rootProject, String... tasks) {
        InvocationResult run = runTasksWithConfigurationCache(gradle, rootProject, true, true, tasks);
        assertThat(run).output().contains("Configuration cache entry stored.");
        return run;
    }

    /**
     * Runs tasks with configuration cache, verifying that the cache is stored.
     *
     * @param gradle the Gradle invoker
     * @param rootProject the root project
     * @param tasks the tasks to execute
     * @return the result of the run
     */
    protected InvocationResult runTasksWithConfigurationCache(
            GradleInvoker gradle, RootProject rootProject, String... tasks) {
        InvocationResult run = runTasksWithConfigurationCache(gradle, rootProject, true, false, tasks);
        assertThat(run).output().contains("Configuration cache entry stored.");
        return run;
    }

    /**
     * Runs tasks with configuration cache with options to clean up cache directory after run
     * and to expect failure.
     *
     * @param gradle the Gradle invoker
     * @param rootProject the root project
     * @param cleanUp whether to clean up the configuration cache directory after the run
     * @param fail whether to expect the build to fail
     * @param tasks the tasks to execute
     * @return the result of the run
     */
    protected InvocationResult runTasksWithConfigurationCache(
            GradleInvoker gradle, RootProject rootProject, boolean cleanUp, boolean fail, String... tasks) {
        // Combine tasks with configuration cache flag
        String[] allArgs = new String[tasks.length + 1];
        System.arraycopy(tasks, 0, allArgs, 0, tasks.length);
        allArgs[tasks.length] = "--configuration-cache";

        InvocationResult run;
        if (fail) {
            run = gradle.withArgs(allArgs).buildsWithFailure();
        } else {
            run = gradle.withArgs(allArgs).buildsSuccessfully();
        }

        Path configCacheDir = rootProject.path().resolve(".gradle/configuration-cache");
        if (cleanUp && configCacheDir.toFile().exists()) {
            try {
                FileUtils.deleteDirectory(configCacheDir.toFile());
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to delete configuration cache directory", e);
            }
        }
        if (cleanUp && configCacheDir.toFile().exists()) {
            // AssertionError is appropriate for testing utilities
            @SuppressWarnings("ThrowError")
            AssertionError error = new AssertionError("Configuration cache directory was not deleted");
            throw error;
        }

        return run;
    }
}
