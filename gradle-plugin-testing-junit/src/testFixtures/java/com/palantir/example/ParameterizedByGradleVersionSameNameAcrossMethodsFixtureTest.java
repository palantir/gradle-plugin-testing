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
import com.palantir.gradle.testing.junit.InjectByGradleVersion;
import com.palantir.gradle.testing.junit.ParameterizedByGradleVersion;
import com.palantir.gradle.testing.junit.WhenVersion;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Fixture test verifying the same parameter name can be used on both @BeforeEach and @Test methods.
 */
@GradlePluginTests
public final class ParameterizedByGradleVersionSameNameAcrossMethodsFixtureTest {

    private String setupBehavior;

    @BeforeEach
    @ParameterizedByGradleVersion(
            name = "behavior",
            when = @WhenVersion(lessThan = "8.0", stringValue = "setup-old"),
            otherwiseString = "setup-new")
    void setup(RootProject rootProject, @InjectByGradleVersion String behavior) {
        this.setupBehavior = behavior;
        rootProject.buildGradle().plugins().add("java");
    }

    @Test
    @ParameterizedByGradleVersion(
            name = "behavior",
            when = @WhenVersion(lessThan = "8.0", stringValue = "test-old"),
            otherwiseString = "test-new")
    void test_same_name_on_both_methods(
            GradleInvoker gradleInvoker, RootProject rootProject, @InjectByGradleVersion String behavior) {
        rootProject.buildGradle().append("""
            import org.gradle.util.GradleVersion
            println "GradleVersion: ${GradleVersion.current().version}"
            println "SetupBehavior: %s"
            println "TestBehavior: %s"
            """, setupBehavior, behavior);

        String output = gradleInvoker.withArgs().buildsSuccessfully().output();
        throw new RuntimeException(
                "setupBehavior=" + setupBehavior + "|testBehavior=" + behavior + "|output=" + output);
    }
}
