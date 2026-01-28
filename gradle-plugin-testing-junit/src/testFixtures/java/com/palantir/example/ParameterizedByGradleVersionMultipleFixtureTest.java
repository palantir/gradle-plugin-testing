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
import org.junit.jupiter.api.Test;

/**
 * Fixture test for verifying multiple stacked ParameterizedByGradleVersion annotations.
 *
 * <p>This test is executed by the end-to-end test ParameterizedByGradleVersionTest using JUnit Platform TestKit.
 */
@GradlePluginTests
public final class ParameterizedByGradleVersionMultipleFixtureTest {

    @ParameterizedByGradleVersion(
            otherwise = "otherwise",
            value = {
                @WhenVersion(
                        lessThan = "9.3.0",
                        value = {"lessThan1", "lessThan2"}),
                @WhenVersion(equalTo = "8.14.3", value = "equal")
            })
    @ParameterizedByGradleVersion(
            otherwise = {"3", "4"},
            value =
                    @WhenVersion(
                            lessThan = "9.3.0",
                            value = {"1", "2"}))
    void test_one(GradleInvoker gradleInvoker, RootProject rootProject, String behavior, String maxInt) {
        rootProject.buildGradle().append("""
            import org.gradle.util.GradleVersion
            println "GradleVersion: ${GradleVersion.current().version}"
            println "Behavior: %s"
            println "MaxInt: %s"
            """, behavior, maxInt);

        String output = gradleInvoker.withArgs().buildsSuccessfully().output();
        throw new RuntimeException("behavior=" + behavior + "|maxInt=" + maxInt + "|output=" + output);
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
