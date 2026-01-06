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
import com.palantir.gradle.testing.junit.WithGradleVersionsGreaterThan;
import com.palantir.gradle.testing.junit.WithGradleVersionsGreaterThanOrEqualTo;
import com.palantir.gradle.testing.junit.WithGradleVersionsLessThan;
import com.palantir.gradle.testing.junit.WithGradleVersionsLessThanOrEqualTo;
import com.palantir.gradle.testing.junit.WithOnlyGradleVersions;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.Test;

/**
 * Test fixture for testing combined version constraints.
 * This fixture is designed to be run with base versions 7.6.5, 8.0, 8.5 via configuration parameter.
 */
@GradlePluginTests
public class CombinedVersionConstraintsFixtureTest {

    @Test
    @WithGradleVersionsGreaterThanOrEqualTo("8.0")
    @WithGradleVersionsLessThan("8.5")
    void test_range_8_0_to_8_5_exclusive(GradleInvoker gradleInvoker, RootProject rootProject) {
        // Should only run on 8.0 (>= 8.0 and < 8.5)
        rootProject.buildGradle().append("""
            import org.gradle.util.GradleVersion
            println "GradleVersion: ${GradleVersion.current().version}"
            """);

        throw new RuntimeException(gradleInvoker.withArgs().buildsSuccessfully().output());
    }

    @Test
    @WithGradleVersionsGreaterThan("7.6.5")
    @WithGradleVersionsLessThanOrEqualTo("8.5")
    void test_range_after_7_6_5_to_8_5_inclusive(GradleInvoker gradleInvoker, RootProject rootProject) {
        // Should run on 8.0 and 8.5 (> 7.6.5 and <= 8.5)
        rootProject.buildGradle().append("""
            import org.gradle.util.GradleVersion
            println "GradleVersion: ${GradleVersion.current().version}"
            """);

        throw new RuntimeException(gradleInvoker.withArgs().buildsSuccessfully().output());
    }

    @Test
    @WithGradleVersionsGreaterThan("8.0")
    @WithGradleVersionsLessThan("8.5")
    void test_range_exclusive_both_sides_no_match(GradleInvoker gradleInvoker, RootProject rootProject) {
        // Should not run on any version (> 8.0 and < 8.5, but no versions in matrix between)
        rootProject.buildGradle().append("""
            import org.gradle.util.GradleVersion
            println "GradleVersion: ${GradleVersion.current().version}"
            """);

        throw new RuntimeException(gradleInvoker.withArgs().buildsSuccessfully().output());
    }

    @Test
    @WithOnlyGradleVersions({"8.0", "8.5"})
    @WithGradleVersionsLessThanOrEqualTo("8.0")
    void test_only_with_less_than_or_equal(GradleInvoker gradleInvoker, RootProject rootProject) {
        // Should only run on 8.0 (in {8.0, 8.5} and <= 8.0)
        rootProject.buildGradle().append("""
            import org.gradle.util.GradleVersion
            println "GradleVersion: ${GradleVersion.current().version}"
            """);

        throw new RuntimeException(gradleInvoker.withArgs().buildsSuccessfully().output());
    }

    @Test
    @WithGradleVersionsGreaterThan("9.0")
    @WithGradleVersionsLessThan("7.0")
    void test_impossible_range(GradleInvoker gradleInvoker, RootProject rootProject) {
        // Should never run - impossible range (> 9.0 AND < 7.0)
        rootProject.buildGradle().append("""
            import org.gradle.util.GradleVersion
            println "GradleVersion: ${GradleVersion.current().version}"
            """);

        throw new RuntimeException(gradleInvoker.withArgs().buildsSuccessfully().output());
    }
}
