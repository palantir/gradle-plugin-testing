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

import com.palantir.example.ParameterizedByGradleVersionFixtureTest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Event;

final class ParameterizedByGradleVersionTest {

    @Nested
    class SingleParameter {
        @Test
        void gradle_7_6_4_runs_less_than_8() {
            EngineExecutionResults results =
                    runFixture(ParameterizedByGradleVersionFixtureTest.SingleParameter.class, "7.6.4");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(getExceptionMessages(finished))
                    .satisfiesExactlyInAnyOrder(
                            msg -> {
                                assertThat(msg).contains("behavior=less than 8");
                                assertThat(msg).contains("GradleVersion: 7.6.4");
                            },
                            msg -> assertThat(msg).contains("GradleVersion: 7.6.4"));
            assertThat(results.testEvents().skipped().count()).isZero();
        }

        @Test
        void gradle_8_14_3_runs_8_x() {
            EngineExecutionResults results =
                    runFixture(ParameterizedByGradleVersionFixtureTest.SingleParameter.class, "8.14.3");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(getExceptionMessages(finished))
                    .satisfiesExactlyInAnyOrder(
                            msg -> {
                                assertThat(msg).contains("behavior=8.x");
                                assertThat(msg).contains("GradleVersion: 8.14.3");
                            },
                            msg -> assertThat(msg).contains("GradleVersion: 8.14.3"));
            assertThat(results.testEvents().skipped().count()).isZero();
        }

        @Test
        void gradle_9_3_0_runs_9_and_up() {
            EngineExecutionResults results =
                    runFixture(ParameterizedByGradleVersionFixtureTest.SingleParameter.class, "9.3.0");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(getExceptionMessages(finished))
                    .satisfiesExactlyInAnyOrder(
                            msg -> {
                                assertThat(msg).contains("behavior=9 and up");
                                assertThat(msg).contains("GradleVersion: 9.3.0");
                            },
                            msg -> assertThat(msg).contains("GradleVersion: 9.3.0"));
            assertThat(results.testEvents().skipped().count()).isZero();
        }
    }

    @Nested
    class WithAdditionalVersion {
        @Test
        void runs_with_correct_values_for_method_level_additional_version() {
            EngineExecutionResults results =
                    runFixture(ParameterizedByGradleVersionFixtureTest.WithMethodLevelAdditionalVersion.class, "7.6.4");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(getExceptionMessages(finished))
                    .satisfiesExactlyInAnyOrder(
                            msg -> {
                                assertThat(msg).contains("behavior=old");
                                assertThat(msg).contains("GradleVersion: 7.6.4");
                            },
                            msg -> {
                                assertThat(msg).contains("behavior=new");
                                assertThat(msg).contains("GradleVersion: 8.5");
                            });
            assertThat(results.testEvents().skipped().count()).isZero();
        }
    }

    @Nested
    class WithBeforeEach {
        @Test
        void before_each_receives_correct_value() {
            EngineExecutionResults results =
                    runFixture(ParameterizedByGradleVersionFixtureTest.WithBeforeEach.class, "7.6.4");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(getExceptionMessages(finished)).satisfiesExactlyInAnyOrder(msg -> {
                assertThat(msg).contains("behavior=old");
            });
            assertThat(results.testEvents().skipped().count()).isZero();
        }
    }

    @Nested
    class WithMultipleAnnotations {
        @Test
        void multiple_parameters_receive_correct_values_for_old_gradle() {
            EngineExecutionResults results =
                    runFixture(ParameterizedByGradleVersionFixtureTest.MultipleAnnotations.class, "7.6.4");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(getExceptionMessages(finished)).satisfiesExactlyInAnyOrder(msg -> {
                assertThat(msg).contains("style=old");
                assertThat(msg).contains("format=classic");
                assertThat(msg).contains("GradleVersion: 7.6.4");
            });
            assertThat(results.testEvents().skipped().count()).isZero();
        }

        @Test
        void multiple_parameters_receive_correct_values_for_gradle_8() {
            EngineExecutionResults results =
                    runFixture(ParameterizedByGradleVersionFixtureTest.MultipleAnnotations.class, "8.14.3");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(getExceptionMessages(finished)).satisfiesExactlyInAnyOrder(msg -> {
                assertThat(msg).contains("style=new");
                assertThat(msg).contains("format=classic");
                assertThat(msg).contains("GradleVersion: 8.14.3");
            });
            assertThat(results.testEvents().skipped().count()).isZero();
        }

        @Test
        void multiple_parameters_receive_correct_values_for_gradle_9() {
            EngineExecutionResults results =
                    runFixture(ParameterizedByGradleVersionFixtureTest.MultipleAnnotations.class, "9.3.0");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(getExceptionMessages(finished)).satisfiesExactlyInAnyOrder(msg -> {
                assertThat(msg).contains("style=new");
                assertThat(msg).contains("format=modern");
                assertThat(msg).contains("GradleVersion: 9.3.0");
            });
            assertThat(results.testEvents().skipped().count()).isZero();
        }
    }

    @Nested
    class SameNameAcrossMethods {
        @Test
        void same_name_allowed_on_before_each_and_test_for_old_gradle() {
            EngineExecutionResults results =
                    runFixture(ParameterizedByGradleVersionFixtureTest.SameNameAcrossMethods.class, "7.6.4");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(getExceptionMessages(finished)).satisfiesExactlyInAnyOrder(msg -> {
                assertThat(msg).contains("setupBehavior=setup-old");
                assertThat(msg).contains("testBehavior=test-old");
                assertThat(msg).contains("GradleVersion: 7.6.4");
            });
            assertThat(results.testEvents().skipped().count()).isZero();
        }

        @Test
        void same_name_allowed_on_before_each_and_test_for_new_gradle() {
            EngineExecutionResults results =
                    runFixture(ParameterizedByGradleVersionFixtureTest.SameNameAcrossMethods.class, "8.14.3");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(getExceptionMessages(finished)).satisfiesExactlyInAnyOrder(msg -> {
                assertThat(msg).contains("setupBehavior=setup-new");
                assertThat(msg).contains("testBehavior=test-new");
                assertThat(msg).contains("GradleVersion: 8.14.3");
            });
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

    private static List<String> getExceptionMessages(List<Event> events) {
        return events.stream()
                .map(event -> event.getPayload(TestExecutionResult.class))
                .<TestExecutionResult>mapMulti(Optional::ifPresent)
                .filter(result -> result.getStatus() == Status.FAILED)
                .map(result -> result.getThrowable().map(Throwable::getMessage).orElse(""))
                .toList();
    }
}
