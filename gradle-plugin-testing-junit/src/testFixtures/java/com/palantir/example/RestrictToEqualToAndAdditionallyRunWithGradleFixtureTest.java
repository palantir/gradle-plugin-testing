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
import com.palantir.gradle.testing.junit.AdditionallyRunWithGradle;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.junit.RestrictToGradleVersionsEqualTo;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.Test;

/**
 * Test fixture for testing {@link RestrictToGradleVersionsEqualTo} combined with {@link AdditionallyRunWithGradle}.
 * This is in a separate fixture so that the {@link AdditionallyRunWithGradle} doesn't affect the test matrix of
 * other tests.
 */
@GradlePluginTests
public class RestrictToEqualToAndAdditionallyRunWithGradleFixtureTest {

    @Test
    @AdditionallyRunWithGradle("8.5")
    @RestrictToGradleVersionsEqualTo("8.5")
    void test_with_both_annotations_adding_and_restricting(GradleInvoker gradleInvoker, RootProject rootProject) {
        rootProject.buildGradle().append("""
            import org.gradle.util.GradleVersion
            println "GradleVersion: ${GradleVersion.current().version}"
            """);

        throw new RuntimeException(gradleInvoker.withArgs().buildsSuccessfully().output());
    }
}
