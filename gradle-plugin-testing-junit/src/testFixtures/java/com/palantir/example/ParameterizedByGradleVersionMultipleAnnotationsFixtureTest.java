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
 * Fixture test for verifying multiple @ParameterizedByGradleVersion annotations with different names.
 */
@GradlePluginTests
public final class ParameterizedByGradleVersionMultipleAnnotationsFixtureTest {

    @Test
    @ParameterizedByGradleVersion(
            name = "style",
            when = @WhenVersion(lessThan = "8.0", stringValue = "old"),
            otherwiseString = "new")
    @ParameterizedByGradleVersion(
            name = "format",
            when = @WhenVersion(lessThan = "9.0", stringValue = "classic"),
            otherwiseString = "modern")
    void test_with_two_parameters(GradleInvoker gradleInvoker, RootProject rootProject, String style, String format) {
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
