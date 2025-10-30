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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This test class validates that SubProject injection is restricted to @Test methods.
 * The test will fail at class initialization if a SubProject parameter is used in @BeforeEach.
 */
@GradlePluginTests
class SubProjectLifecycleRestrictionTest {
    // Uncomment the following to verify the restriction works:
    // @BeforeEach
    // void beforeEach(SubProject subProject) {
    //     // This should fail with IllegalStateException
    // }

    @Test
    void subproject_works_in_test_method(SubProject api) {
        assertThat(api.path()).isNotNull();
    }
}
