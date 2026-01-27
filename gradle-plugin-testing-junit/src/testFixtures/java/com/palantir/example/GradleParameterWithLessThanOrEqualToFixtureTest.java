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
import com.palantir.gradle.testing.junit.ForVersion;
import com.palantir.gradle.testing.junit.GradleParameter;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;

/**
 * Fixture test for verifying GradleParameter lessThanOrEqualTo functionality.
 *
 * <p>This test is executed by the end-to-end test GradleParameterTest using JUnit Platform TestKit.
 * The test throws RuntimeException with the parameter values to communicate them back to the E2E test.
 */
@GradlePluginTests
public final class GradleParameterWithLessThanOrEqualToFixtureTest {

    @GradleParameter(
            name = "behavior",
            otherwiseStrings = "otherwise",
            value = {
                @ForVersion(lessThanOrEqualTo = "8.14.3", strings = "lessThanOrEqualTo"),
            })
    void test_lessThanOrEqualTo(GradleInvoker gradleInvoker, RootProject rootProject, String behavior) {
        rootProject.buildGradle().append("""
            import org.gradle.util.GradleVersion
            println "GradleVersion: ${GradleVersion.current().version}"
            println "Behavior: %s"
            """, behavior);

        String output = gradleInvoker.withArgs().buildsSuccessfully().output();
        throw new RuntimeException("behavior=" + behavior + "|output=" + output);
    }
}
