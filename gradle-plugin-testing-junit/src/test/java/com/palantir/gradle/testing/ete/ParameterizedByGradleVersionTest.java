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

import static org.junit.platform.testkit.engine.EventConditions.reportEntry;

import com.palantir.example.ParameterizedByGradleVersionFixtureTest;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

final class ParameterizedByGradleVersionTest {

    @Nested
    class SingleParameter {
        @ParameterizedTest
        @MethodSource("gradleVersions")
        void runs_correct_behavior_for_version(String gradleVersion, String expectedBehavior) {
            EngineExecutionResults results =
                    runFixture(ParameterizedByGradleVersionFixtureTest.SingleParameter.class, gradleVersion);

            results.allEvents()
                    .assertStatistics(stats -> stats.skipped(0).failed(0))
                    .assertEventsMatchLooselyInOrder(
                            reportEntry(Map.of("behavior", expectedBehavior)),
                            reportEntry(Map.of("behavior", "other")));
        }

        static Stream<Arguments> gradleVersions() {
            return Stream.of(
                    Arguments.of("7.6.4", "less than 8"),
                    Arguments.of("8.14.3", "8.x"),
                    Arguments.of("9.3.0", "9 and up"));
        }
    }

    @Nested
    class WithAdditionalVersion {
        @Test
        void runs_with_correct_values_for_method_level_additional_version() {
            EngineExecutionResults results =
                    runFixture(ParameterizedByGradleVersionFixtureTest.WithMethodLevelAdditionalVersion.class, "7.6.4");

            results.allEvents()
                    .assertStatistics(stats -> stats.skipped(0).failed(0))
                    .assertEventsMatchLooselyInOrder(
                            reportEntry(Map.of("behavior", "old")), reportEntry(Map.of("behavior", "new")));
        }
    }

    @Nested
    class WithBeforeEach {
        @ParameterizedTest
        @MethodSource("gradleVersions")
        void before_each_receives_correct_value(String gradleVersion, String expectedBehavior) {
            EngineExecutionResults results =
                    runFixture(ParameterizedByGradleVersionFixtureTest.WithBeforeEach.class, gradleVersion);

            results.allEvents()
                    .assertStatistics(stats -> stats.skipped(0).failed(0))
                    .assertEventsMatchLooselyInOrder(reportEntry(Map.of("behavior", expectedBehavior)));
        }

        static Stream<Arguments> gradleVersions() {
            return Stream.of(Arguments.of("7.6.4", "old"), Arguments.of("8.14.3", "new"));
        }
    }

    @Nested
    class WithMultipleAnnotations {
        @ParameterizedTest
        @MethodSource("gradleVersions")
        void multiple_parameters_receive_correct_values(
                String gradleVersion, String expectedStyle, String expectedFormat) {
            EngineExecutionResults results =
                    runFixture(ParameterizedByGradleVersionFixtureTest.MultipleAnnotations.class, gradleVersion);

            results.allEvents()
                    .assertStatistics(stats -> stats.skipped(0).failed(0))
                    .assertEventsMatchLooselyInOrder(
                            reportEntry(Map.of("style", expectedStyle)), reportEntry(Map.of("format", expectedFormat)));
        }

        static Stream<Arguments> gradleVersions() {
            return Stream.of(
                    Arguments.of("7.6.4", "old", "classic"),
                    Arguments.of("8.14.3", "new", "classic"),
                    Arguments.of("9.3.0", "new", "modern"));
        }
    }

    @Nested
    class SameNameAcrossMethods {
        @Test
        void same_name_allowed_on_before_each_and_test_for_old_gradle() {
            EngineExecutionResults results =
                    runFixture(ParameterizedByGradleVersionFixtureTest.SameNameAcrossMethods.class, "7.6.4");

            results.allEvents()
                    .assertStatistics(stats -> stats.skipped(0).failed(0))
                    .assertEventsMatchLooselyInOrder(
                            reportEntry(Map.of("setupBehavior", "setup-old")),
                            reportEntry(Map.of("testBehavior", "test-old")));
        }
    }

    private static EngineExecutionResults runFixture(Class<?> fixtureClass, String gradleVersion) {
        return EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(fixtureClass))
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", gradleVersion)
                .configurationParameter("com.palantir.gradle.testing.configuration_cache_enabled", "false")
                .execute();
    }
}
