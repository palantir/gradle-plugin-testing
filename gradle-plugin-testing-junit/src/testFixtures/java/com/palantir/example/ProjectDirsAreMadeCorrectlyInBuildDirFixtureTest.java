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

import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// This is a test fixture, not a real test. See ProjectDirsAreMadeCorrectlyInBuildDirTest which uses it.
@GradlePluginTests
public class ProjectDirsAreMadeCorrectlyInBuildDirFixtureTest {
    @Test
    void regular_test(RootProject rootProject) {}

    @ParameterizedTest(name = "{index}: {0}")
    @ValueSource(strings = {"foo", "bar"})
    void parameterized_test(String param, RootProject rootProject) {}

    @Nested
    class NestedClass {
        @ParameterizedTest(name = "{index}: {0}")
        @ValueSource(strings = {"foo", "bar"})
        void nested_parameterized_test(String param, RootProject rootProject) {}

        @Nested
        class DoublyNestedClass {
            @ParameterizedTest(name = "{index}: {0}")
            @ValueSource(strings = {"foo", "bar"})
            void doubly_nested_parameterized_test(String param, RootProject rootProject) {}
        }
    }
}
