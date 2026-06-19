/*
 * (c) Copyright 2026 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.gradle.testing.integration;

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.execution.Options;
import com.palantir.gradle.testing.junit.DisabledTestingProperty;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import java.util.Map;
import org.junit.jupiter.api.Test;

@GradlePluginTests
public class TestingPropertiesTest {

    @Test
    void root_project_parameter(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            println "hello from ${project.property("__TESTING_FOO")}"
            """);

        Options fooOptions = Options.builder()
                .testingEnvironmentVariables(Map.of("FOO", "foo"))
                .build();
        gradle.with(fooOptions).buildsSuccessfully().assertThat().output().contains("hello from foo");

        Options barOptions = fooOptions
                .asBuilder()
                .putTestingEnvironmentVariables("FOO", "bar")
                .build();
        gradle.with(barOptions).buildsSuccessfully().assertThat().output().contains("hello from bar");
    }

    @Test
    @DisabledTestingProperty
    void can_override_testing_property(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            println "testing value is ${project.property("__TESTING")}"
            """);

        gradle.withArgs("help").buildsSuccessfully().assertThat().output().contains("testing value is false");
    }
}
