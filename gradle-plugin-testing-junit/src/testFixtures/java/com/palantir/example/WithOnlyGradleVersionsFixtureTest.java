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

package com.palantir.example;

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.junit.WithOnlyGradleVersions;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.Test;

/**
 * Test fixture for testing {@link WithOnlyGradleVersions} annotation behavior.
 * This fixture is designed to be run with base versions 7.6.5 and 8.0 via configuration parameter.
 */
@GradlePluginTests
public class WithOnlyGradleVersionsFixtureTest {

    @Test
    void test_without_only_annotation(GradleInvoker gradleInvoker, RootProject rootProject) {
        rootProject.buildGradle().append("""
            import org.gradle.util.GradleVersion
            println "GradleVersion: ${GradleVersion.current().version}"
            """);

        // This exception is just so we can pass the output back up to the JUnit testkit-based test
        throw new RuntimeException(gradleInvoker.withArgs().buildsSuccessfully().output());
    }

    @Test
    @WithOnlyGradleVersions("8.0")
    void test_with_only_annotation_filtering_to_existing_version(GradleInvoker gradleInvoker, RootProject rootProject) {
        rootProject.buildGradle().append("""
            import org.gradle.util.GradleVersion
            println "GradleVersion: ${GradleVersion.current().version}"
            """);

        throw new RuntimeException(gradleInvoker.withArgs().buildsSuccessfully().output());
    }

    @Test
    @WithOnlyGradleVersions("8.5")
    void test_with_only_annotation_filtering_to_nonexisting_version(
            GradleInvoker gradleInvoker, RootProject rootProject) {
        rootProject.buildGradle().append("""
            import org.gradle.util.GradleVersion
            println "GradleVersion: ${GradleVersion.current().version}"
            """);

        throw new RuntimeException(gradleInvoker.withArgs().buildsSuccessfully().output());
    }
}
