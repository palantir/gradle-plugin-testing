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
import com.palantir.example.ParameterizedByGradleVersionMultipleFixtureTest;
import com.palantir.example.ParameterizedByGradleVersionWithAdditionalVersionFixtureTest;
import com.palantir.example.ParameterizedByGradleVersionWithDisabledConfigurationCacheFixtureTest;
import com.palantir.example.ParameterizedByGradleVersionWithLessThanOrEqualToFixtureTest;
import com.palantir.example.ParameterizedByGradleVersionWithMethodLevelAdditionalVersionFixtureTest;
import java.util.List;
import java.util.function.Consumer;
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
        void gradle_7_6_4_runs_lessThan_values() {
            EngineExecutionResults results = runFixture(ParameterizedByGradleVersionFixtureTest.class, "7.6.4");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(finished).hasSize(4);
            assertThat(finished)
                    .satisfiesExactlyInAnyOrder(
                            forEvent("7.6.4", "lessThan1", "behavior=lessThan1"),
                            forEvent("7.6.4", "lessThan2", "behavior=lessThan2"),
                            forEvent("7.6.4", "2", "behavior=2"),
                            forEvent("7.6.4", "other test"));
            assertThat(results.testEvents().skipped().count()).isZero();
        }

        @Test
        void gradle_8_14_3_runs_lessThan_and_equalTo_values() {
            EngineExecutionResults results = runFixture(ParameterizedByGradleVersionFixtureTest.class, "8.14.3");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(finished).hasSize(5);
            assertThat(finished)
                    .satisfiesExactlyInAnyOrder(
                            forEvent("8.14.3", "lessThan1", "behavior=lessThan1"),
                            forEvent("8.14.3", "lessThan2", "behavior=lessThan2"),
                            forEvent("8.14.3", "equal", "behavior=equal"),
                            forEvent("8.14.3", "2", "behavior=2"),
                            forEvent("8.14.3", "other test"));
            assertThat(results.testEvents().skipped().count()).isZero();
        }

        @Test
        void gradle_9_3_0_runs_otherwise_values() {
            EngineExecutionResults results = runFixture(ParameterizedByGradleVersionFixtureTest.class, "9.3.0");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(finished).hasSize(3);
            assertThat(finished)
                    .satisfiesExactlyInAnyOrder(
                            forEvent("9.3.0", "otherwise", "behavior=otherwise"),
                            forEvent("9.3.0", "1", "behavior=1"),
                            forEvent("9.3.0", "other test"));
            assertThat(results.testEvents().skipped().count()).isZero();
        }
    }

    @Nested
    class WithAdditionalVersion {
        @Test
        void runs_with_correct_values_for_each_version() {
            EngineExecutionResults results =
                    runFixture(ParameterizedByGradleVersionWithAdditionalVersionFixtureTest.class, "7.6.4");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(finished).hasSize(2);
            assertThat(finished)
                    .satisfiesExactlyInAnyOrder(
                            forEvent("7.6.4", "old", "behavior=old"), forEvent("8.5", "new", "behavior=new"));
            assertThat(results.testEvents().skipped().count()).isZero();
        }

        @Test
        void runs_with_correct_values_for_method_level_additional_version() {
            EngineExecutionResults results =
                    runFixture(ParameterizedByGradleVersionWithMethodLevelAdditionalVersionFixtureTest.class, "7.6.4");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(finished).hasSize(2);
            assertThat(finished)
                    .satisfiesExactlyInAnyOrder(
                            forEvent("7.6.4", "old", "behavior=old"), forEvent("8.5", "new", "behavior=new"));
            assertThat(results.testEvents().skipped().count()).isZero();
        }
    }

    @Nested
    class WithDisabledConfigurationCache {
        @Test
        void runs_with_configuration_cache_disabled() {
            EngineExecutionResults results = runFixture(
                    ParameterizedByGradleVersionWithDisabledConfigurationCacheFixtureTest.class, "7.6.4", "true");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(finished).hasSize(1);
            assertThat(finished)
                    .satisfiesExactlyInAnyOrder(
                            forEvent("7.6.4", "old", "behavior=old", "isConfigurationCacheRequested=false"));
            assertThat(results.testEvents().skipped().count()).isZero();
        }
    }

    @Nested
    class WithLessThanOrEqualTo {
        @Test
        void gradle_7_6_4_matches_lessThanOrEqualTo_8_14_3() {
            EngineExecutionResults results =
                    runFixture(ParameterizedByGradleVersionWithLessThanOrEqualToFixtureTest.class, "7.6.4");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(finished).hasSize(1);
            assertThat(finished)
                    .satisfiesExactlyInAnyOrder(forEvent("7.6.4", "lessThanOrEqualTo", "behavior=lessThanOrEqualTo"));
            assertThat(results.testEvents().skipped().count()).isZero();
        }

        @Test
        void gradle_8_14_3_matches_lessThanOrEqualTo_8_14_3() {
            EngineExecutionResults results =
                    runFixture(ParameterizedByGradleVersionWithLessThanOrEqualToFixtureTest.class, "8.14.3");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(finished).hasSize(1);
            assertThat(finished)
                    .satisfiesExactlyInAnyOrder(forEvent("8.14.3", "lessThanOrEqualTo", "behavior=lessThanOrEqualTo"));
            assertThat(results.testEvents().skipped().count()).isZero();
        }

        @Test
        void gradle_9_3_0_uses_otherwise() {
            EngineExecutionResults results =
                    runFixture(ParameterizedByGradleVersionWithLessThanOrEqualToFixtureTest.class, "9.3.0");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(finished).hasSize(1);
            assertThat(finished).satisfiesExactlyInAnyOrder(forEvent("9.3.0", "otherwise", "behavior=otherwise"));
            assertThat(results.testEvents().skipped().count()).isZero();
        }
    }

    @Nested
    class MultipleParameters {
        @Test
        void gradle_7_6_4_creates_cartesian_product() {
            EngineExecutionResults results = runFixture(ParameterizedByGradleVersionMultipleFixtureTest.class, "7.6.4");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(finished).hasSize(5);
            assertThat(finished)
                    .satisfiesExactlyInAnyOrder(
                            forEvent("7.6.4", "lessThan1, 1", "behavior=lessThan1", "maxInt=1"),
                            forEvent("7.6.4", "lessThan1, 2", "behavior=lessThan1", "maxInt=2"),
                            forEvent("7.6.4", "lessThan2, 1", "behavior=lessThan2", "maxInt=1"),
                            forEvent("7.6.4", "lessThan2, 2", "behavior=lessThan2", "maxInt=2"),
                            forEvent("7.6.4", "other test"));
            assertThat(results.testEvents().skipped().count()).isZero();
        }

        @Test
        void gradle_8_14_3_creates_cartesian_product_with_equalTo() {
            EngineExecutionResults results =
                    runFixture(ParameterizedByGradleVersionMultipleFixtureTest.class, "8.14.3");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(finished).hasSize(7);
            assertThat(finished)
                    .satisfiesExactlyInAnyOrder(
                            forEvent("8.14.3", "lessThan1, 1", "behavior=lessThan1", "maxInt=1"),
                            forEvent("8.14.3", "lessThan1, 2", "behavior=lessThan1", "maxInt=2"),
                            forEvent("8.14.3", "lessThan2, 1", "behavior=lessThan2", "maxInt=1"),
                            forEvent("8.14.3", "lessThan2, 2", "behavior=lessThan2", "maxInt=2"),
                            forEvent("8.14.3", "equal, 1", "behavior=equal", "maxInt=1"),
                            forEvent("8.14.3", "equal, 2", "behavior=equal", "maxInt=2"),
                            forEvent("8.14.3", "other test"));
            assertThat(results.testEvents().skipped().count()).isZero();
        }

        @Test
        void gradle_9_3_0_creates_cartesian_product_with_otherwise() {
            EngineExecutionResults results = runFixture(ParameterizedByGradleVersionMultipleFixtureTest.class, "9.3.0");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(finished).hasSize(3);
            assertThat(finished)
                    .satisfiesExactlyInAnyOrder(
                            forEvent("9.3.0", "otherwise, 3", "behavior=otherwise", "maxInt=3"),
                            forEvent("9.3.0", "otherwise, 4", "behavior=otherwise", "maxInt=4"),
                            forEvent("9.3.0", "other test"));
            assertThat(results.testEvents().skipped().count()).isZero();
        }
    }

    private static EngineExecutionResults runFixture(Class<?> fixtureClass, String gradleVersion) {
        return runFixture(fixtureClass, gradleVersion, "false");
    }

    private static EngineExecutionResults runFixture(
            Class<?> fixtureClass, String gradleVersion, String configurationCacheEnabled) {
        return EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(fixtureClass))
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", gradleVersion)
                .configurationParameter(
                        "com.palantir.gradle.testing.configuration_cache_enabled", configurationCacheEnabled)
                .execute();
    }

    /** Creates an assertion for a failed test event with the given display name and message substrings. */
    private static Consumer<Event> forEvent(
            String gradleVersion, String displayNameContains, String... messageContains) {
        return event -> {
            assertThat(event.getTestDescriptor().getDisplayName()).contains(displayNameContains);

            assertThat(event.getPayload(TestExecutionResult.class)).hasValueSatisfying(result -> {
                assertThat(result.getStatus()).isEqualTo(Status.FAILED);
                assertThat(result.getThrowable()).hasValueSatisfying(throwable -> {
                    for (String substring : messageContains) {
                        assertThat(throwable.getMessage()).contains(substring);
                    }
                    assertThat(throwable.getMessage()).contains("GradleVersion: " + gradleVersion);
                });
            });
        };
    }
}
