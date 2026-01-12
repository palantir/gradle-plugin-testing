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

import com.palantir.example.AdditionallyRunWithGradleFixtureTest;
import com.palantir.example.MethodLevelAdditionallyRunWithGradleFixtureTest;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Event;

final class AdditionallyRunWithGradleTest {
    @Test
    void additionally_run_with_gradle_annotation_adds_versions_to_test_matrix() {
        EngineExecutionResults executionResults = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(AdditionallyRunWithGradleFixtureTest.class))
                // Base version configured via parameter
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", "7.6.5")
                .configurationParameter("com.palantir.gradle.testing.configuration_cache_enabled", "false")
                .execute();

        List<Event> finished = executionResults.testEvents().finished().stream().toList();

        assertThat(finished)
                .satisfiesExactlyInAnyOrder(
                        // from config parameter
                        ranWithGradleVersion("7.6.5"),
                        // from @AdditionallyRunWithGradle
                        ranWithGradleVersion("8.0"),
                        // from @AdditionallyRunWithGradle
                        ranWithGradleVersion("8.5"));
    }

    @Test
    void duplicate_versions_are_deduplicated() {
        EngineExecutionResults executionResults = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(AdditionallyRunWithGradleFixtureTest.class))
                // 8.0 is both in config and in @AdditionallyRunWithGradle
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", "8.0")
                .configurationParameter("com.palantir.gradle.testing.configuration_cache_enabled", "false")
                .execute();

        List<Event> finished = executionResults.testEvents().finished().stream().toList();

        assertThat(finished)
                .satisfiesExactlyInAnyOrder(
                        // 8.0 is in both config and @AdditionallyRunWithGradle, but only runs once
                        ranWithGradleVersion("8.0"),
                        // from @AdditionallyRunWithGradle
                        ranWithGradleVersion("8.5"));
    }

    @Test
    void method_level_additionally_run_with_gradle_only_apply_to_annotated_method() {
        EngineExecutionResults executionResults = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(MethodLevelAdditionallyRunWithGradleFixtureTest.class))
                // Base version configured via parameter
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", "7.6.5")
                .configurationParameter("com.palantir.gradle.testing.configuration_cache_enabled", "false")
                .execute();

        List<Event> finished = executionResults.testEvents().finished().stream().toList();
        List<Event> skipped = executionResults.testEvents().skipped().stream().toList();

        assertThat(finished)
                .satisfiesExactlyInAnyOrder(
                        // "test without method annotation" runs on base (7.6.5) and class-level (8.0)
                        ranWithNameAndVersion("test without method annotation", "7.6.5"),
                        ranWithNameAndVersion("test without method annotation", "8.0"),
                        // "test with method annotation" runs on base (7.6.5), class-level (8.0), and method-level (8.5)
                        ranWithNameAndVersion("test with method annotation", "7.6.5"),
                        ranWithNameAndVersion("test with method annotation", "8.0"),
                        ranWithNameAndVersion("test with method annotation", "8.5"));

        // Method without annotation is skipped for 8.5 (only runs on base + class versions)
        assertThat(skipped).satisfiesExactly(skippedWithNameAndVersion("test without method annotation", "8.5"));
    }

    private static Consumer<Event> ranWithGradleVersion(String gradleVersion) {
        return event -> Assertions.assertThatRanWithCorrectGradleVersion(
                AdditionallyRunWithGradleFixtureTest.class, event, gradleVersion);
    }

    private static Consumer<Event> ranWithNameAndVersion(String displayNameContains, String gradleVersion) {
        return event -> {
            assertThat(event.getTestDescriptor().getDisplayName()).contains(displayNameContains);
            Assertions.assertThatRanWithCorrectGradleVersion(
                    MethodLevelAdditionallyRunWithGradleFixtureTest.class, event, gradleVersion, displayNameContains);
        };
    }

    private static Consumer<Event> skippedWithNameAndVersion(String displayNameContains, String gradleVersion) {
        return event -> {
            assertThat(event.getTestDescriptor().getDisplayName()).contains(displayNameContains);
            assertThat(event.getTestDescriptor().getParent().map(TestDescriptor::getDisplayName))
                    .hasValue("Gradle " + gradleVersion);
        };
    }
}
