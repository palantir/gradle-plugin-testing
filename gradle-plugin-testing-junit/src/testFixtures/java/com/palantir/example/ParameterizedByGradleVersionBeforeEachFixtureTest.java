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
import com.palantir.gradle.testing.junit.ParameterizedByGradleVersion;
import com.palantir.gradle.testing.junit.WhenVersion;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Fixture test for verifying @ParameterizedByGradleVersion works with @BeforeEach.
 */
@GradlePluginTests
public final class ParameterizedByGradleVersionBeforeEachFixtureTest {

    private String capturedBehavior;

    @BeforeEach
    @ParameterizedByGradleVersion(
            name = "behavior",
            when = @WhenVersion(lessThan = "8.0", stringValue = "old"),
            otherwiseString = "new")
    void setup(RootProject rootProject, String behavior) {
        this.capturedBehavior = behavior;
        rootProject.buildGradle().plugins().add("java");
    }

    @Test
    void test_uses_value_from_before_each(GradleInvoker gradleInvoker, RootProject rootProject) {
        rootProject.buildGradle().append("""
            import org.gradle.util.GradleVersion
            println "GradleVersion: ${GradleVersion.current().version}"
            println "Behavior: %s"
            """, capturedBehavior);

        String output = gradleInvoker.withArgs().buildsSuccessfully().output();
        throw new RuntimeException("behavior=" + capturedBehavior + "|output=" + output);
    }
}
