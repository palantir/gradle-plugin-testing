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

package com.palantir.gradle.testing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@GradlePluginTests
final class ParameterizedTestUsagesTest {
    @Nested
    class NestedClass {
        @ParameterizedTest(name = "{index}: {0}")
        @ValueSource(strings = "foo")
        void a_test_with_params_has_one_directory_per_param(String param, RootProject rootProject) {
            assertThat(param).isIn("foo");

            assertThat(rootProject.path().toString())
                    .describedAs("Each parameter gets it's own test directory, handles 'bad' characters like colons")
                    .contains("ParameterizedTestUsagesTest/NestedClass/"
                            + "a test with params has one directory per param/"
                            + "1_ foo");
        }
    }
}
