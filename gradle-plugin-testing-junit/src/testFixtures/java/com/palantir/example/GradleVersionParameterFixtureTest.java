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

import com.palantir.gradle.testing.execution.GradleVersion;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import org.junit.jupiter.api.Test;

@GradlePluginTests
public class GradleVersionParameterFixtureTest {
    @Test
    void test_receives_gradle_version(GradleVersion gradleVersion) {
        // Throw the version so the test runner can verify it was injected correctly
        throw new RuntimeException("GradleVersion: " + gradleVersion.version());
    }
}
