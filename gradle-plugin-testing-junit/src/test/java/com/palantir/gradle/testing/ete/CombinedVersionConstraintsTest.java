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

import com.palantir.example.CombinedVersionConstraintsFixtureTest;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Event;

final class CombinedVersionConstraintsTest {

    @Test
    void combined_constraints_filter_correctly() {
        EngineExecutionResults executionResults = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(CombinedVersionConstraintsFixtureTest.class))
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", "7.6.5,8.0,8.5")
                .configurationParameter("com.palantir.gradle.testing.configuration_cache_enabled", "false")
                .execute();

        List<Event> finished = executionResults.testEvents().finished().stream().toList();
        List<Event> skipped = executionResults.testEvents().skipped().stream().toList();

        assertThat(finished)
                .satisfiesExactlyInAnyOrder(
                        // test_range_8_0_to_8_5_exclusive: >= 8.0 AND < 8.5 -> only 8.0
                        ranWithNameAndVersion("test range 8 0 to 8 5 exclusive", "8.0"),
                        // test_range_after_7_6_5_to_8_5_inclusive: > 7.6.5 AND <= 8.5 -> 8.0 and 8.5
                        ranWithNameAndVersion("test range after 7 6 5 to 8 5 inclusive", "8.0"),
                        ranWithNameAndVersion("test range after 7 6 5 to 8 5 inclusive", "8.5"),
                        // test_only_with_less_than_or_equal: only {8.0, 8.5} AND <= 8.0 -> only 8.0
                        ranWithNameAndVersion("test only with less than or equal", "8.0"));

        assertThat(skipped)
                .satisfiesExactlyInAnyOrder(
                        // test_range_8_0_to_8_5_exclusive skipped for 7.6.5 and 8.5
                        skippedWithNameAndVersion("test range 8 0 to 8 5 exclusive", "7.6.5"),
                        skippedWithNameAndVersion("test range 8 0 to 8 5 exclusive", "8.5"),
                        // test_range_after_7_6_5_to_8_5_inclusive skipped for 7.6.5
                        skippedWithNameAndVersion("test range after 7 6 5 to 8 5 inclusive", "7.6.5"),
                        // test_range_exclusive_both_sides_no_match: > 8.0 AND < 8.5 -> none (no versions in between)
                        skippedWithNameAndVersion("test range exclusive both sides no match", "7.6.5"),
                        skippedWithNameAndVersion("test range exclusive both sides no match", "8.0"),
                        skippedWithNameAndVersion("test range exclusive both sides no match", "8.5"),
                        // test_only_with_less_than_or_equal skipped for 7.6.5 and 8.5
                        skippedWithNameAndVersion("test only with less than or equal", "7.6.5"),
                        skippedWithNameAndVersion("test only with less than or equal", "8.5"),
                        // test_impossible_range: > 9.0 AND < 7.0 -> impossible, all skipped
                        skippedWithNameAndVersion("test impossible range", "7.6.5"),
                        skippedWithNameAndVersion("test impossible range", "8.0"),
                        skippedWithNameAndVersion("test impossible range", "8.5"));
    }

    private static Consumer<Event> ranWithNameAndVersion(String displayNameContains, String gradleVersion) {
        return event -> {
            assertThat(event.getTestDescriptor().getDisplayName()).contains(displayNameContains);
            Assertions.assertThatRanWithCorrectGradleVersion(
                    CombinedVersionConstraintsFixtureTest.class, event, gradleVersion, displayNameContains);
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
