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

package com.palantir.gradle.testing.ete;

import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.example.DuplicateDecoratorTests;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Event;

public class DuplicatedAnnotationsFailTest {

    @Test
    void runs_with_duplicated_annotations() {
        EngineExecutionResults executionResults = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(DuplicateDecoratorTests.class))
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", "8.14.3")
                .configurationParameter(
                        "com.palantir.gradle.testing.configuration_cache_enabled", String.valueOf(false))
                .execute();

        List<Event> testEvents = executionResults.testEvents().failed().list();
        assertThat(testEvents).hasSize(2);

        // Test 1: Method-level duplicate on duplicate_decorator_throws_exception
        Event failedTest1 = testEvents.get(0);
        assertExceptionMessage(
                failedTest1,
                "Decorator annotation @WithDecorator is already registered in a parent class. The same decorator"
                        + " annotation cannot be applied at both class and method level.",
                "Please remove it from the test method `duplicate_decorator_throws_exception`");

        // Test 2: Method-level duplicate on another_test_with_duplicated_decorator
        Event failedTest2 = testEvents.get(1);
        assertExceptionMessage(
                failedTest2,
                "Decorator annotation @WithDecorator is already registered in a parent class.",
                "Please remove it from the test method `another_test_with_duplicated_decorator`");

        List<Event> containerEvents =
                executionResults.containerEvents().failed().list();
        assertThat(containerEvents).hasSize(1);
        // Test 3: Class-level duplicate on TestClassWithDuplicateDecorator
        Event failedTest3 = containerEvents.get(0);
        assertExceptionMessage(
                failedTest3,
                "Decorator annotation @WithDecorator is already registered in a parent class. The same decorator"
                        + " annotation cannot be applied at multiple class levels.",
                "Please remove the extra annotation from the class `TestClassWithDuplicateDecorator`");
    }

    private static void assertExceptionMessage(Event failedTest3, String reason, String explanation) {
        assertThat(failedTest3.getPayload(TestExecutionResult.class)).hasValueSatisfying(testExecutionResult -> {
            assertThat(testExecutionResult.getStatus()).isEqualTo(TestExecutionResult.Status.FAILED);
            Assertions.assertThatTestFailureExceptionMessageContains(testExecutionResult, reason);
            Assertions.assertThatTestFailureExceptionMessageContains(testExecutionResult, explanation);
        });
    }
}
