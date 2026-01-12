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

import com.palantir.example.ClassLevelRestrictToGradleVersionsEqualToFixtureTest;
import com.palantir.example.RestrictToEqualToAndAdditionallyRunWithGradleFixtureTest;
import com.palantir.example.RestrictToGradleVersionsEqualToFixtureTest;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Event;

final class RestrictToGradleVersionsEqualToTest {

    @Test
    void restrict_to_gradle_versions_equal_to_filters_to_specified_version() {
        EngineExecutionResults executionResults = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(RestrictToGradleVersionsEqualToFixtureTest.class))
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", "7.6.5,8.0")
                .configurationParameter("com.palantir.gradle.testing.configuration_cache_enabled", "false")
                .execute();

        List<Event> finished = executionResults.testEvents().finished().stream().toList();
        List<Event> skipped = executionResults.testEvents().skipped().stream().toList();

        assertThat(finished)
                .satisfiesExactlyInAnyOrder(
                        // test_without_restrict_annotation runs on both base versions
                        ranWithNameAndVersion(
                                RestrictToGradleVersionsEqualToFixtureTest.class,
                                "test without restrict annotation",
                                "7.6.5"),
                        ranWithNameAndVersion(
                                RestrictToGradleVersionsEqualToFixtureTest.class,
                                "test without restrict annotation",
                                "8.0"),
                        // test_with_restrict_annotation_filtering_to_existing_version only runs on 8.0
                        ranWithNameAndVersion(
                                RestrictToGradleVersionsEqualToFixtureTest.class,
                                "test with restrict annotation filtering to existing version",
                                "8.0"));

        assertThat(skipped).hasSize(3);
        assertThat(skipped)
                .satisfiesExactlyInAnyOrder(
                        // 7.6.5 skipped for "restrict to 8.0" test
                        skippedWithNameAndVersion(
                                "test with restrict annotation filtering to existing version", "7.6.5"),
                        // 7.6.5 skipped for "restrict to 8.5" test (nonexistent in matrix)
                        skippedWithNameAndVersion(
                                "test with restrict annotation filtering to nonexisting version", "7.6.5"),
                        // 8.0 skipped for "restrict to 8.5" test (nonexistent in matrix)
                        skippedWithNameAndVersion(
                                "test with restrict annotation filtering to nonexisting version", "8.0"));
    }

    @Test
    void restrict_to_equal_to_and_additionally_run_with_gradle_combined_adds_then_filters() {
        EngineExecutionResults executionResults = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(
                        RestrictToEqualToAndAdditionallyRunWithGradleFixtureTest.class))
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", "7.6.5,8.0")
                .configurationParameter("com.palantir.gradle.testing.configuration_cache_enabled", "false")
                .execute();

        List<Event> finished = executionResults.testEvents().finished().stream().toList();
        List<Event> skipped = executionResults.testEvents().skipped().stream().toList();

        assertThat(finished)
                .satisfiesExactly(ranWithNameAndVersion(
                        RestrictToEqualToAndAdditionallyRunWithGradleFixtureTest.class,
                        "test with both annotations adding and restricting",
                        "8.5"));

        assertThat(skipped).hasSize(2);
        assertThat(skipped)
                .satisfiesExactlyInAnyOrder(
                        skippedWithNameAndVersion("test with both annotations adding and restricting", "7.6.5"),
                        skippedWithNameAndVersion("test with both annotations adding and restricting", "8.0"));
    }

    @Test
    void class_level_restrict_to_gradle_versions_equal_to_filters_matrix_upfront() {
        EngineExecutionResults executionResults = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(ClassLevelRestrictToGradleVersionsEqualToFixtureTest.class))
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", "7.6.5,8.0")
                .configurationParameter("com.palantir.gradle.testing.configuration_cache_enabled", "false")
                .execute();

        List<Event> finished = executionResults.testEvents().finished().stream().toList();
        List<Event> skipped = executionResults.testEvents().skipped().stream().toList();

        // Class-level @RestrictToGradleVersionsEqualTo("8.0") filters matrix to only 8.0
        // No tests should be skipped - 7.6.5 is not even in the matrix
        assertThat(finished)
                .satisfiesExactly(ranWithNameAndVersion(
                        ClassLevelRestrictToGradleVersionsEqualToFixtureTest.class,
                        "test runs only on restricted version",
                        "8.0"));

        assertThat(skipped).isEmpty();
    }

    public static Consumer<Event> ranWithNameAndVersion(
            Class<?> testClass, String displayNameContains, String gradleVersion) {
        return event -> {
            assertThat(event.getTestDescriptor().getDisplayName()).contains(displayNameContains);
            Assertions.assertThatRanWithCorrectGradleVersion(testClass, event, gradleVersion, displayNameContains);
        };
    }

    public static Consumer<Event> skippedWithNameAndVersion(String displayNameContains, String gradleVersion) {
        return event -> {
            assertThat(event.getTestDescriptor().getDisplayName()).contains(displayNameContains);
            assertThat(event.getTestDescriptor().getParent().map(TestDescriptor::getDisplayName))
                    .hasValue("Gradle " + gradleVersion);
        };
    }
}
