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

package com.palantir.example;

import com.palantir.example.Decorators.WithDecorator;
import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.junit.DisabledConfigurationCache;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests that verify duplicate decorator detection.
 */
@WithDecorator
@GradlePluginTests
@DisabledConfigurationCache("Testing decorator validation")
public class DuplicateDecoratorTests {

    @Test
    @WithDecorator
    void duplicate_decorator_throws_exception(GradleInvoker invoker, RootProject rootProject) {
        // This test will fail during parameter resolution because @WithDecorator
        // is applied at both class level and method level.

        // If we reach here, the validation failed
        throw new AssertionError("Expected IllegalStateException to be thrown for duplicate decorator");
    }

    @Nested
    class OtherTestClass {

        @WithDecorator
        @Test
        void another_test_with_duplicated_decorator(GradleInvoker invoker, RootProject rootProject) {
            // This test will fail during parameter resolution because @WithDecorator
            // is applied at both class level and method level.

            // If we reach here, the validation failed
            throw new AssertionError("Expected IllegalStateException to be thrown for duplicate decorator");
        }
    }

    @WithDecorator
    @Nested
    class TestClassWithDuplicateDecorator {

        @Test
        void simple_test(GradleInvoker invoker, RootProject rootProject) {
            // This test will fail during parameter resolution because @WithDecorator
            // is applied at both class levels.

            // If we reach here, the validation failed
            throw new AssertionError("Expected IllegalStateException to be thrown for duplicate decorator");
        }
    }
}
