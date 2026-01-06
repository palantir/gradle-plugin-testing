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

import static com.palantir.gradle.testing.ete.Assertions.ranWithNameAndVersion;
import static com.palantir.gradle.testing.ete.Assertions.skippedWithNameAndVersion;
import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.example.ClassLevelWithOnlyGradleVersionsFixtureTest;
import com.palantir.example.WithOnlyAndWithGradleVersionsFixtureTest;
import com.palantir.example.WithOnlyGradleVersionsFixtureTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Event;

final class WithOnlyGradleVersionsTest {

    @Test
    void with_only_gradle_versions_filters_to_specified_version() {
        EngineExecutionResults executionResults = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(WithOnlyGradleVersionsFixtureTest.class))
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", "7.6.5,8.0")
                .configurationParameter("com.palantir.gradle.testing.configuration_cache_enabled", "false")
                .execute();

        List<Event> finished = executionResults.testEvents().finished().stream().toList();
        List<Event> skipped = executionResults.testEvents().skipped().stream().toList();

        // test_without_only_annotation runs on both base versions (7.6.5, 8.0)
        // test_with_only_annotation_filtering_to_existing_version runs only on 8.0 (filtered)
        // test_with_only_annotation_filtering_to_nonexisting_version is skipped on both (8.5 not in matrix)

        assertThat(finished)
                .satisfiesExactlyInAnyOrder(
                        // test_without_only_annotation runs on both base versions
                        ranWithNameAndVersion(
                                WithOnlyGradleVersionsFixtureTest.class, "test without only annotation", "7.6.5"),
                        ranWithNameAndVersion(
                                WithOnlyGradleVersionsFixtureTest.class, "test without only annotation", "8.0"),
                        // test_with_only_annotation_filtering_to_existing_version only runs on 8.0
                        ranWithNameAndVersion(
                                WithOnlyGradleVersionsFixtureTest.class,
                                "test with only annotation filtering to existing version",
                                "8.0"));

        // test_with_only_annotation_filtering_to_existing_version is skipped on 7.6.5
        // test_with_only_annotation_filtering_to_nonexisting_version is skipped on both versions
        assertThat(skipped).hasSize(3);
        assertThat(skipped)
                .satisfiesExactlyInAnyOrder(
                        // 7.6.5 skipped for "only 8.0" test
                        skippedWithNameAndVersion("test with only annotation filtering to existing version", "7.6.5"),
                        // 7.6.5 skipped for "only 8.5" test (nonexistent in matrix)
                        skippedWithNameAndVersion(
                                "test with only annotation filtering to nonexisting version", "7.6.5"),
                        // 8.0 skipped for "only 8.5" test (nonexistent in matrix)
                        skippedWithNameAndVersion("test with only annotation filtering to nonexisting version", "8.0"));
    }

    @Test
    void with_only_and_with_gradle_versions_combined_adds_then_filters() {
        EngineExecutionResults executionResults = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(WithOnlyAndWithGradleVersionsFixtureTest.class))
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", "7.6.5,8.0")
                .configurationParameter("com.palantir.gradle.testing.configuration_cache_enabled", "false")
                .execute();

        List<Event> finished = executionResults.testEvents().finished().stream().toList();
        List<Event> skipped = executionResults.testEvents().skipped().stream().toList();

        // @WithGradleVersions("8.5") adds 8.5 to matrix, @WithOnlyGradleVersions("8.5") filters to only 8.5
        assertThat(finished)
                .satisfiesExactly(ranWithNameAndVersion(
                        WithOnlyAndWithGradleVersionsFixtureTest.class,
                        "test with both annotations adding and filtering",
                        "8.5"));

        // 7.6.5 and 8.0 are skipped because @WithOnlyGradleVersions filters them out
        assertThat(skipped).hasSize(2);
        assertThat(skipped)
                .satisfiesExactlyInAnyOrder(
                        skippedWithNameAndVersion("test with both annotations adding and filtering", "7.6.5"),
                        skippedWithNameAndVersion("test with both annotations adding and filtering", "8.0"));
    }

    @Test
    void class_level_with_only_gradle_versions_filters_matrix_upfront() {
        EngineExecutionResults executionResults = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(ClassLevelWithOnlyGradleVersionsFixtureTest.class))
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", "7.6.5,8.0")
                .configurationParameter("com.palantir.gradle.testing.configuration_cache_enabled", "false")
                .execute();

        List<Event> finished = executionResults.testEvents().finished().stream().toList();
        List<Event> skipped = executionResults.testEvents().skipped().stream().toList();

        // Class-level @WithOnlyGradleVersions("8.0") filters matrix to only 8.0
        // No tests should be skipped - 7.6.5 is not even in the matrix
        assertThat(finished)
                .satisfiesExactly(ranWithNameAndVersion(
                        ClassLevelWithOnlyGradleVersionsFixtureTest.class,
                        "test runs only on filtered version",
                        "8.0"));

        assertThat(skipped).isEmpty();
    }
}
