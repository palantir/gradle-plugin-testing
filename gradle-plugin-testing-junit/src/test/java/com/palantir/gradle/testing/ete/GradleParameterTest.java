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

package com.palantir.gradle.testing.ete;

import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.example.GradleParameterFixtureTest;
import com.palantir.example.GradleParameterMultipleFixtureTest;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Event;

final class GradleParameterTest {

    @Nested
    class SingleParameter {
        /**
         * Tests with Gradle 7.6.4 (lessThan 9.3.0):
         * - test_one should run twice with behavior = lessThan1, lessThan2
         * - test_two should run once with behavior = 2
         * - other_test should run once
         */
        @Test
        void gradle_7_6_4_runs_lessThan_values() {
            EngineExecutionResults results = runFixture(GradleParameterFixtureTest.class, "7.6.4");

            List<Event> finished = results.testEvents().finished().stream().toList();

            // test_one: 2 invocations (lessThan1, lessThan2)
            // test_two: 1 invocation (2)
            // other_test: 1 invocation
            assertThat(finished).hasSize(4);

            assertThat(finished)
                    .satisfiesExactlyInAnyOrder(
                            eventWithBehavior("test_one", "lessThan1", "7.6.4"),
                            eventWithBehavior("test_one", "lessThan2", "7.6.4"),
                            eventWithIntBehavior("test_two", 2, "7.6.4"),
                            eventForOtherTest("7.6.4"));

            // Verify no skipped tests
            assertThat(results.testEvents().skipped().count()).isZero();
        }

        /**
         * Tests with Gradle 8.14.3 (lessThan 9.3.0 AND equalTo 8.14.3):
         * - test_one should run THREE times: lessThan1, lessThan2, equal
         * - test_two should run once with behavior = 2
         * - other_test should run once
         */
        @Test
        void gradle_8_14_3_runs_lessThan_and_equalTo_values() {
            EngineExecutionResults results = runFixture(GradleParameterFixtureTest.class, "8.14.3");

            List<Event> finished = results.testEvents().finished().stream().toList();

            // test_one: 3 invocations (lessThan1, lessThan2, equal)
            // test_two: 1 invocation (2)
            // other_test: 1 invocation
            assertThat(finished).hasSize(5);

            assertThat(finished)
                    .satisfiesExactlyInAnyOrder(
                            eventWithBehavior("test_one", "lessThan1", "8.14.3"),
                            eventWithBehavior("test_one", "lessThan2", "8.14.3"),
                            eventWithBehavior("test_one", "equal", "8.14.3"),
                            eventWithIntBehavior("test_two", 2, "8.14.3"),
                            eventForOtherTest("8.14.3"));

            // Verify no skipped tests
            assertThat(results.testEvents().skipped().count()).isZero();
        }

        /**
         * Tests with Gradle 9.3.0 (not lessThan 9.3.0, uses otherwise):
         * - test_one should run once with behavior = otherwise
         * - test_two should run once with behavior = 1
         * - other_test should run once
         */
        @Test
        void gradle_9_3_0_runs_otherwise_values() {
            EngineExecutionResults results = runFixture(GradleParameterFixtureTest.class, "9.3.0");

            List<Event> finished = results.testEvents().finished().stream().toList();

            // test_one: 1 invocation (otherwise)
            // test_two: 1 invocation (1)
            // other_test: 1 invocation
            assertThat(finished).hasSize(3);

            assertThat(finished)
                    .satisfiesExactlyInAnyOrder(
                            eventWithBehavior("test_one", "otherwise", "9.3.0"),
                            eventWithIntBehavior("test_two", 1, "9.3.0"),
                            eventForOtherTest("9.3.0"));

            // Verify no skipped tests
            assertThat(results.testEvents().skipped().count()).isZero();
        }
    }

    @Nested
    class MultipleParameters {
        /**
         * Tests with Gradle 7.6.4:
         * - test_one: Cartesian product of {lessThan1, lessThan2} x {1, 2} = 4 invocations
         * - other_test: 1 invocation
         */
        @Test
        void gradle_7_6_4_creates_cartesian_product() {
            EngineExecutionResults results = runFixture(GradleParameterMultipleFixtureTest.class, "7.6.4");

            List<Event> finished = results.testEvents().finished().stream().toList();

            // test_one: 4 invocations (2 behaviors x 2 maxInts)
            // other_test: 1 invocation
            assertThat(finished).hasSize(5);

            assertThat(finished)
                    .satisfiesExactlyInAnyOrder(
                            eventWithBehaviorAndMaxInt("lessThan1", 1, "7.6.4"),
                            eventWithBehaviorAndMaxInt("lessThan1", 2, "7.6.4"),
                            eventWithBehaviorAndMaxInt("lessThan2", 1, "7.6.4"),
                            eventWithBehaviorAndMaxInt("lessThan2", 2, "7.6.4"),
                            eventForOtherTest("7.6.4"));

            // Verify no skipped tests
            assertThat(results.testEvents().skipped().count()).isZero();
        }

        /**
         * Tests with Gradle 8.14.3:
         * - test_one: Cartesian product of {lessThan1, lessThan2, equal} x {1, 2} = 6 invocations
         * - other_test: 1 invocation
         */
        @Test
        void gradle_8_14_3_creates_cartesian_product_with_equalTo() {
            EngineExecutionResults results = runFixture(GradleParameterMultipleFixtureTest.class, "8.14.3");

            List<Event> finished = results.testEvents().finished().stream().toList();

            // test_one: 6 invocations (3 behaviors x 2 maxInts)
            // other_test: 1 invocation
            assertThat(finished).hasSize(7);

            assertThat(finished)
                    .satisfiesExactlyInAnyOrder(
                            eventWithBehaviorAndMaxInt("lessThan1", 1, "8.14.3"),
                            eventWithBehaviorAndMaxInt("lessThan1", 2, "8.14.3"),
                            eventWithBehaviorAndMaxInt("lessThan2", 1, "8.14.3"),
                            eventWithBehaviorAndMaxInt("lessThan2", 2, "8.14.3"),
                            eventWithBehaviorAndMaxInt("equal", 1, "8.14.3"),
                            eventWithBehaviorAndMaxInt("equal", 2, "8.14.3"),
                            eventForOtherTest("8.14.3"));

            // Verify no skipped tests
            assertThat(results.testEvents().skipped().count()).isZero();
        }

        /**
         * Tests with Gradle 9.3.0:
         * - test_one: Cartesian product of {otherwise} x {3, 4} = 2 invocations
         * - other_test: 1 invocation
         */
        @Test
        void gradle_9_3_0_creates_cartesian_product_with_otherwise() {
            EngineExecutionResults results = runFixture(GradleParameterMultipleFixtureTest.class, "9.3.0");

            List<Event> finished = results.testEvents().finished().stream().toList();

            // test_one: 2 invocations (1 behavior x 2 maxInts)
            // other_test: 1 invocation
            assertThat(finished).hasSize(3);

            assertThat(finished)
                    .satisfiesExactlyInAnyOrder(
                            eventWithBehaviorAndMaxInt("otherwise", 3, "9.3.0"),
                            eventWithBehaviorAndMaxInt("otherwise", 4, "9.3.0"),
                            eventForOtherTest("9.3.0"));

            // Verify no skipped tests
            assertThat(results.testEvents().skipped().count()).isZero();
        }
    }

    private static EngineExecutionResults runFixture(Class<?> fixtureClass, String gradleVersion) {
        return EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(fixtureClass))
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", gradleVersion)
                .configurationParameter("com.palantir.gradle.testing.configuration_cache_enabled", "false")
                .execute();
    }

    private static Consumer<Event> eventWithBehavior(String testName, String behavior, String gradleVersion) {
        return event -> {
            // For test template invocations, the display name is the parameter value
            // The test name is in the parent descriptor
            assertThat(event.getTestDescriptor().getDisplayName()).isEqualTo(behavior);
            event.getTestDescriptor().getParent().ifPresent(parent -> assertThat(parent.getDisplayName())
                    .contains(testName.replace("_", " ")));
            assertThat(event.getPayload(TestExecutionResult.class)).hasValueSatisfying(result -> {
                assertThat(result.getStatus()).isEqualTo(Status.FAILED);
                assertThat(result.getThrowable()).hasValueSatisfying(throwable -> {
                    assertThat(throwable.getMessage()).contains("behavior=" + behavior);
                    assertThat(throwable.getMessage()).contains("GradleVersion: " + gradleVersion);
                });
            });
            assertGradleVersionParent(event, gradleVersion);
        };
    }

    private static Consumer<Event> eventWithIntBehavior(String testName, int behavior, String gradleVersion) {
        return event -> {
            // For test template invocations, the display name is the parameter value
            assertThat(event.getTestDescriptor().getDisplayName()).isEqualTo(String.valueOf(behavior));
            event.getTestDescriptor().getParent().ifPresent(parent -> assertThat(parent.getDisplayName())
                    .contains(testName.replace("_", " ")));
            assertThat(event.getPayload(TestExecutionResult.class)).hasValueSatisfying(result -> {
                assertThat(result.getStatus()).isEqualTo(Status.FAILED);
                assertThat(result.getThrowable()).hasValueSatisfying(throwable -> {
                    assertThat(throwable.getMessage()).contains("behavior=" + behavior);
                    assertThat(throwable.getMessage()).contains("GradleVersion: " + gradleVersion);
                });
            });
            assertGradleVersionParent(event, gradleVersion);
        };
    }

    private static Consumer<Event> eventWithBehaviorAndMaxInt(String behavior, int maxInt, String gradleVersion) {
        return event -> {
            // For multiple parameters, display name is "behavior=X, maxInt=Y"
            assertThat(event.getTestDescriptor().getDisplayName())
                    .isEqualTo("behavior=" + behavior + ", maxInt=" + maxInt);
            event.getTestDescriptor().getParent().ifPresent(parent -> assertThat(parent.getDisplayName())
                    .contains("test one"));
            assertThat(event.getPayload(TestExecutionResult.class)).hasValueSatisfying(result -> {
                assertThat(result.getStatus()).isEqualTo(Status.FAILED);
                assertThat(result.getThrowable()).hasValueSatisfying(throwable -> {
                    assertThat(throwable.getMessage()).contains("behavior=" + behavior);
                    assertThat(throwable.getMessage()).contains("maxInt=" + maxInt);
                    assertThat(throwable.getMessage()).contains("GradleVersion: " + gradleVersion);
                });
            });
            assertGradleVersionParent(event, gradleVersion);
        };
    }

    private static Consumer<Event> eventForOtherTest(String gradleVersion) {
        return event -> {
            assertThat(event.getTestDescriptor().getDisplayName()).contains("other test");
            assertThat(event.getPayload(TestExecutionResult.class)).hasValueSatisfying(result -> {
                assertThat(result.getStatus()).isEqualTo(Status.FAILED);
                assertThat(result.getThrowable()).hasValueSatisfying(throwable -> {
                    assertThat(throwable.getMessage()).contains("GradleVersion: " + gradleVersion);
                });
            });
            assertGradleVersionParent(event, gradleVersion);
        };
    }

    private static void assertGradleVersionParent(Event event, String gradleVersion) {
        // Walk up the parent hierarchy to find "Gradle X.Y.Z"
        TestDescriptor descriptor = event.getTestDescriptor();
        boolean foundGradleVersion = false;
        StringBuilder hierarchy = new StringBuilder();
        while (descriptor.getParent().isPresent()) {
            descriptor = descriptor.getParent().get();
            hierarchy.append(" -> ").append(descriptor.getDisplayName());
            if (descriptor.getDisplayName().equals("Gradle " + gradleVersion)) {
                foundGradleVersion = true;
                break;
            }
        }
        assertThat(foundGradleVersion)
                .as("Expected to find 'Gradle %s' in parent hierarchy: %s", gradleVersion, hierarchy)
                .isTrue();
    }
}
