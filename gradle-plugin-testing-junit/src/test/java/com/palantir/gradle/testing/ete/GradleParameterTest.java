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
import com.palantir.example.GradleParameterWithAdditionalVersionFixtureTest;
import com.palantir.example.GradleParameterWithDisabledConfigurationCacheFixtureTest;
import com.palantir.example.GradleParameterWithMethodLevelAdditionalVersionFixtureTest;
import java.util.ArrayList;
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

            assertThat(finished).hasSize(4);
            assertThat(finished)
                    .satisfiesExactlyInAnyOrder(
                            eventFor("7.6.4")
                                    .displayName("lessThan1")
                                    .parentContains("test one")
                                    .messageContains("behavior=lessThan1"),
                            eventFor("7.6.4")
                                    .displayName("lessThan2")
                                    .parentContains("test one")
                                    .messageContains("behavior=lessThan2"),
                            eventFor("7.6.4")
                                    .displayName("2")
                                    .parentContains("test two")
                                    .messageContains("behavior=2"),
                            eventFor("7.6.4").displayNameContains("other test"));
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

            assertThat(finished).hasSize(5);
            assertThat(finished)
                    .satisfiesExactlyInAnyOrder(
                            eventFor("8.14.3")
                                    .displayName("lessThan1")
                                    .parentContains("test one")
                                    .messageContains("behavior=lessThan1"),
                            eventFor("8.14.3")
                                    .displayName("lessThan2")
                                    .parentContains("test one")
                                    .messageContains("behavior=lessThan2"),
                            eventFor("8.14.3")
                                    .displayName("equal")
                                    .parentContains("test one")
                                    .messageContains("behavior=equal"),
                            eventFor("8.14.3")
                                    .displayName("2")
                                    .parentContains("test two")
                                    .messageContains("behavior=2"),
                            eventFor("8.14.3").displayNameContains("other test"));
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

            assertThat(finished).hasSize(3);
            assertThat(finished)
                    .satisfiesExactlyInAnyOrder(
                            eventFor("9.3.0")
                                    .displayName("otherwise")
                                    .parentContains("test one")
                                    .messageContains("behavior=otherwise"),
                            eventFor("9.3.0")
                                    .displayName("1")
                                    .parentContains("test two")
                                    .messageContains("behavior=1"),
                            eventFor("9.3.0").displayNameContains("other test"));
            assertThat(results.testEvents().skipped().count()).isZero();
        }
    }

    @Nested
    class WithAdditionalVersion {
        @Test
        void runs_with_correct_values_for_each_version() {
            EngineExecutionResults results = runFixture(GradleParameterWithAdditionalVersionFixtureTest.class, "7.6.4");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(finished).hasSize(2);
            assertThat(finished)
                    .satisfiesExactlyInAnyOrder(
                            eventFor("7.6.4").displayName("old").messageContains("behavior=old"),
                            eventFor("8.5").displayName("new").messageContains("behavior=new"));
            assertThat(results.testEvents().skipped().count()).isZero();
        }

        @Test
        void runs_with_correct_values_for_method_level_additional_version() {
            EngineExecutionResults results =
                    runFixture(GradleParameterWithMethodLevelAdditionalVersionFixtureTest.class, "7.6.4");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(finished).hasSize(2);
            assertThat(finished)
                    .satisfiesExactlyInAnyOrder(
                            eventFor("7.6.4").displayName("old").messageContains("behavior=old"),
                            eventFor("8.5").displayName("new").messageContains("behavior=new"));
            assertThat(results.testEvents().skipped().count()).isZero();
        }
    }

    @Nested
    class WithDisabledConfigurationCache {
        @Test
        void runs_with_configuration_cache_disabled() {
            EngineExecutionResults results =
                    runFixture(GradleParameterWithDisabledConfigurationCacheFixtureTest.class, "7.6.4", "true");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(finished).hasSize(1);
            assertThat(finished)
                    .satisfiesExactlyInAnyOrder(eventFor("7.6.4")
                            .displayName("old")
                            .messageContains("behavior=old", "isConfigurationCacheRequested=false"));
            assertThat(results.testEvents().skipped().count()).isZero();
        }
    }

    @Nested
    class MultipleParameters {
        @Test
        void gradle_7_6_4_creates_cartesian_product() {
            EngineExecutionResults results = runFixture(GradleParameterMultipleFixtureTest.class, "7.6.4");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(finished).hasSize(5);
            assertThat(finished)
                    .satisfiesExactlyInAnyOrder(
                            eventFor("7.6.4")
                                    .displayName("behavior=lessThan1, maxInt=1")
                                    .messageContains("behavior=lessThan1", "maxInt=1"),
                            eventFor("7.6.4")
                                    .displayName("behavior=lessThan1, maxInt=2")
                                    .messageContains("behavior=lessThan1", "maxInt=2"),
                            eventFor("7.6.4")
                                    .displayName("behavior=lessThan2, maxInt=1")
                                    .messageContains("behavior=lessThan2", "maxInt=1"),
                            eventFor("7.6.4")
                                    .displayName("behavior=lessThan2, maxInt=2")
                                    .messageContains("behavior=lessThan2", "maxInt=2"),
                            eventFor("7.6.4").displayNameContains("other test"));
            assertThat(results.testEvents().skipped().count()).isZero();
        }

        @Test
        void gradle_8_14_3_creates_cartesian_product_with_equalTo() {
            EngineExecutionResults results = runFixture(GradleParameterMultipleFixtureTest.class, "8.14.3");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(finished).hasSize(7);
            assertThat(finished)
                    .satisfiesExactlyInAnyOrder(
                            eventFor("8.14.3")
                                    .displayName("behavior=lessThan1, maxInt=1")
                                    .messageContains("behavior=lessThan1", "maxInt=1"),
                            eventFor("8.14.3")
                                    .displayName("behavior=lessThan1, maxInt=2")
                                    .messageContains("behavior=lessThan1", "maxInt=2"),
                            eventFor("8.14.3")
                                    .displayName("behavior=lessThan2, maxInt=1")
                                    .messageContains("behavior=lessThan2", "maxInt=1"),
                            eventFor("8.14.3")
                                    .displayName("behavior=lessThan2, maxInt=2")
                                    .messageContains("behavior=lessThan2", "maxInt=2"),
                            eventFor("8.14.3")
                                    .displayName("behavior=equal, maxInt=1")
                                    .messageContains("behavior=equal", "maxInt=1"),
                            eventFor("8.14.3")
                                    .displayName("behavior=equal, maxInt=2")
                                    .messageContains("behavior=equal", "maxInt=2"),
                            eventFor("8.14.3").displayNameContains("other test"));
            assertThat(results.testEvents().skipped().count()).isZero();
        }

        @Test
        void gradle_9_3_0_creates_cartesian_product_with_otherwise() {
            EngineExecutionResults results = runFixture(GradleParameterMultipleFixtureTest.class, "9.3.0");

            List<Event> finished = results.testEvents().finished().stream().toList();

            assertThat(finished).hasSize(3);
            assertThat(finished)
                    .satisfiesExactlyInAnyOrder(
                            eventFor("9.3.0")
                                    .displayName("behavior=otherwise, maxInt=3")
                                    .messageContains("behavior=otherwise", "maxInt=3"),
                            eventFor("9.3.0")
                                    .displayName("behavior=otherwise, maxInt=4")
                                    .messageContains("behavior=otherwise", "maxInt=4"),
                            eventFor("9.3.0").displayNameContains("other test"));
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

    private static EventMatcher eventFor(String gradleVersion) {
        return new EventMatcher(gradleVersion);
    }

    private static final class EventMatcher implements Consumer<Event> {
        private final String gradleVersion;
        private String exactDisplayName;
        private String displayNameContains;
        private String parentContains;
        private final List<String> messageContains = new ArrayList<>();

        EventMatcher(String gradleVersion) {
            this.gradleVersion = gradleVersion;
        }

        EventMatcher displayName(String name) {
            this.exactDisplayName = name;
            return this;
        }

        EventMatcher displayNameContains(String substring) {
            this.displayNameContains = substring;
            return this;
        }

        EventMatcher parentContains(String substring) {
            this.parentContains = substring;
            return this;
        }

        EventMatcher messageContains(String... substrings) {
            this.messageContains.addAll(List.of(substrings));
            return this;
        }

        @Override
        public void accept(Event event) {
            String displayName = event.getTestDescriptor().getDisplayName();

            if (exactDisplayName != null) {
                assertThat(displayName).isEqualTo(exactDisplayName);
            }
            if (displayNameContains != null) {
                assertThat(displayName).contains(displayNameContains);
            }
            if (parentContains != null) {
                event.getTestDescriptor().getParent().ifPresent(parent -> assertThat(parent.getDisplayName())
                        .contains(parentContains));
            }

            assertThat(event.getPayload(TestExecutionResult.class)).hasValueSatisfying(result -> {
                assertThat(result.getStatus()).isEqualTo(Status.FAILED);
                if (!messageContains.isEmpty()) {
                    assertThat(result.getThrowable()).hasValueSatisfying(throwable -> {
                        for (String substring : messageContains) {
                            assertThat(throwable.getMessage()).contains(substring);
                        }
                        assertThat(throwable.getMessage()).contains("GradleVersion: " + gradleVersion);
                    });
                }
            });

            assertGradleVersionInParentHierarchy(event, gradleVersion);
        }

        private static void assertGradleVersionInParentHierarchy(Event event, String gradleVersion) {
            TestDescriptor descriptor = event.getTestDescriptor();
            StringBuilder hierarchy = new StringBuilder();
            while (descriptor.getParent().isPresent()) {
                descriptor = descriptor.getParent().get();
                hierarchy.append(" -> ").append(descriptor.getDisplayName());
                if (descriptor.getDisplayName().equals("Gradle " + gradleVersion)) {
                    return;
                }
            }
            assertThat(false)
                    .as("Expected to find 'Gradle %s' in parent hierarchy: %s", gradleVersion, hierarchy)
                    .isTrue();
        }
    }
}
