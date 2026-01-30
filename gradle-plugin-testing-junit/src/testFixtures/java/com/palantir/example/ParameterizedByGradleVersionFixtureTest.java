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
import com.palantir.gradle.testing.junit.InjectByGradleVersion;
import com.palantir.gradle.testing.junit.ParameterizedByGradleVersion;
import com.palantir.gradle.testing.junit.ParameterizedByGradleVersion.WhenVersion;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public final class ParameterizedByGradleVersionFixtureTest {

    @GradlePluginTests
    public static class SingleParameter {

        @Test
        @ParameterizedByGradleVersion(
                when = {
                    @WhenVersion(lessThan = "8.0", stringValue = "less than 8"),
                    @WhenVersion(lessThan = "9.0", stringValue = "8.x")
                },
                otherwiseString = "9 and up")
        void test_one(GradleInvoker gradleInvoker, RootProject rootProject, @InjectByGradleVersion String behavior) {
            rootProject.buildGradle().append("""
                import org.gradle.util.GradleVersion
                println "GradleVersion: ${GradleVersion.current().version}"
                println "Behavior: %s"
                """, behavior);

            String output = gradleInvoker.withArgs().buildsSuccessfully().output();
            throw new RuntimeException("behavior=" + behavior + "|output=" + output);
        }

        @Test
        void other_test(GradleInvoker gradleInvoker, RootProject rootProject) {
            rootProject.buildGradle().append("""
                import org.gradle.util.GradleVersion
                println "GradleVersion: ${GradleVersion.current().version}"
                println "Behavior: other"
                """);

            String output = gradleInvoker.withArgs().buildsSuccessfully().output();
            throw new RuntimeException("output=" + output);
        }
    }

    @GradlePluginTests
    public static class WithBeforeEach {

        private String capturedBehavior;

        @BeforeEach
        @ParameterizedByGradleVersion(
                when = @WhenVersion(lessThan = "8.0", stringValue = "old"),
                otherwiseString = "new")
        void setup(RootProject rootProject, @InjectByGradleVersion String behavior) {
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

    @GradlePluginTests
    public static class MultipleAnnotations {

        @Test
        @ParameterizedByGradleVersion(
                name = "style",
                when = @WhenVersion(lessThan = "8.0", stringValue = "old"),
                otherwiseString = "new")
        @ParameterizedByGradleVersion(
                name = "format",
                when = @WhenVersion(lessThan = "9.0", stringValue = "classic"),
                otherwiseString = "modern")
        void test_with_two_parameters(
                GradleInvoker gradleInvoker,
                RootProject rootProject,
                @InjectByGradleVersion String style,
                @InjectByGradleVersion String format) {
            rootProject.buildGradle().append("""
                import org.gradle.util.GradleVersion
                println "GradleVersion: ${GradleVersion.current().version}"
                println "Style: %s"
                println "Format: %s"
                """, style, format);

            String output = gradleInvoker.withArgs().buildsSuccessfully().output();
            throw new RuntimeException("style=" + style + "|format=" + format + "|output=" + output);
        }
    }

    @GradlePluginTests
    public static class SameNameAcrossMethods {

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

    @GradlePluginTests
    public static class WithMethodLevelAdditionalVersion {

        @Test
        @ParameterizedByGradleVersion(
                when = @WhenVersion(lessThan = "8.0", stringValue = "old"),
                otherwiseString = "new")
        @AdditionallyRunWithGradle("8.5")
        void test_with_parameter(
                GradleInvoker gradleInvoker, RootProject rootProject, @InjectByGradleVersion String behavior) {
            rootProject.buildGradle().append("""
                import org.gradle.util.GradleVersion
                println "GradleVersion: ${GradleVersion.current().version}"
                println "Behavior: %s"
                """, behavior);

            String output = gradleInvoker.withArgs().buildsSuccessfully().output();
            throw new RuntimeException("behavior=" + behavior + "|output=" + output);
        }
    }
}
