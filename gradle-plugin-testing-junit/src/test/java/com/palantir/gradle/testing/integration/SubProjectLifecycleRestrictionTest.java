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
import com.palantir.gradle.testing.project.SubProject;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * This test class validates that SubProject injection is restricted to test methods.
 * SubProject works in @Test, @ParameterizedTest, @RepeatedTest, etc.
 * Attempting to inject SubProject in @BeforeEach will fail with IllegalStateException.
 */
@GradlePluginTests
class SubProjectLifecycleRestrictionTest {
    // Uncommenting the following will cause the test to fail with IllegalStateException:
    //    @BeforeEach
    //    void beforeEach(SubProject subProject) {
    //        // This will fail: SubProject can only be injected in test methods
    //    }

    @Test
    void subproject_works_in_test_method(SubProject api) {
        assertThat(api.path()).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"value1", "value2"})
    void subproject_works_in_parameterized_test(String param, SubProject service) {
        assertThat(service.path()).isNotNull();
        assertThat(param).isIn("value1", "value2");
    }

    @RepeatedTest(2)
    void subproject_works_in_repeated_test(SubProject worker) {
        assertThat(worker.path()).isNotNull();
    }
}
