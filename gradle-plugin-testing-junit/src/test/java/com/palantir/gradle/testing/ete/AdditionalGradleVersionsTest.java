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

import com.palantir.example.AdditionalGradleVersionsFixtureTest;
import com.palantir.example.MethodLevelAdditionalGradleVersionsFixtureTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Event;

final class AdditionalGradleVersionsTest {
    @Test
    void additional_gradle_versions_annotation_adds_versions_to_test_matrix() {
        EngineExecutionResults executionResults = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(AdditionalGradleVersionsFixtureTest.class))
                // Base version configured via parameter
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", "7.6.5")
                .configurationParameter("com.palantir.gradle.testing.configuration_cache_enabled", "false")
                .execute();

        List<Event> finished = executionResults.testEvents().finished().stream().toList();

        // Should have 3 test runs: 7.6.5 from config + 8.0 and 8.5 from @AdditionalGradleVersions
        assertThat(finished).hasSize(3);

        Assertions.assertThatRanWithCorrectGradleVersion(
                AdditionalGradleVersionsFixtureTest.class, finished.get(0), "7.6.5");
        Assertions.assertThatRanWithCorrectGradleVersion(
                AdditionalGradleVersionsFixtureTest.class, finished.get(1), "8.0");
        Assertions.assertThatRanWithCorrectGradleVersion(
                AdditionalGradleVersionsFixtureTest.class, finished.get(2), "8.5");
    }

    @Test
    void duplicate_versions_are_deduplicated() {
        EngineExecutionResults executionResults = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(AdditionalGradleVersionsFixtureTest.class))
                // 8.0 is both in config and in @AdditionalGradleVersions
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", "8.0")
                .configurationParameter("com.palantir.gradle.testing.configuration_cache_enabled", "false")
                .execute();

        List<Event> finished = executionResults.testEvents().finished().stream().toList();

        // Should have 2 test runs: 8.0 (deduplicated) and 8.5 from @AdditionalGradleVersions
        assertThat(finished).hasSize(2);

        Assertions.assertThatRanWithCorrectGradleVersion(
                AdditionalGradleVersionsFixtureTest.class, finished.get(0), "8.0");
        Assertions.assertThatRanWithCorrectGradleVersion(
                AdditionalGradleVersionsFixtureTest.class, finished.get(1), "8.5");
    }

    @Test
    void method_level_additional_gradle_versions_only_apply_to_annotated_method() {
        EngineExecutionResults executionResults = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(MethodLevelAdditionalGradleVersionsFixtureTest.class))
                // Base version configured via parameter
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", "7.6.5")
                .configurationParameter("com.palantir.gradle.testing.configuration_cache_enabled", "false")
                .execute();

        List<Event> finished = executionResults.testEvents().finished().stream().toList();
        List<Event> skipped = executionResults.testEvents().skipped().stream().toList();

        // Method without annotation: runs with base + class (7.6.5, 8.0) = 2 runs
        // Method with @AdditionalGradleVersions({"8.5"}): runs with base + class + method (7.6.5, 8.0, 8.5) = 3 runs
        // Total finished: 5 tests, skipped: 1 (method without annotation skipped for 8.5)
        assertThat(finished).hasSize(5);
        assertThat(skipped).hasSize(1);

        // Verify the skipped test is the method without annotation running on 8.5
        assertThat(skipped.get(0).getTestDescriptor().getDisplayName()).contains("test without method annotation");
        assertThat(skipped.get(0)
                        .getTestDescriptor()
                        .getParent()
                        .map(TestDescriptor::getDisplayName)
                        .orElse(""))
                .isEqualTo("Gradle 8.5");
    }
}
