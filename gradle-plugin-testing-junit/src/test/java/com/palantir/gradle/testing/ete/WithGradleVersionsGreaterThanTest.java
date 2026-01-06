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

import com.palantir.example.WithGradleVersionsGreaterThanFixtureTest;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Event;

final class WithGradleVersionsGreaterThanTest {

    @Test
    void greater_than_filters_to_versions_above_bound() {
        EngineExecutionResults executionResults = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(WithGradleVersionsGreaterThanFixtureTest.class))
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", "7.6.5,8.0,8.5")
                .configurationParameter("com.palantir.gradle.testing.configuration_cache_enabled", "false")
                .execute();

        List<Event> finished = executionResults.testEvents().finished().stream().toList();
        List<Event> skipped = executionResults.testEvents().skipped().stream().toList();

        assertThat(finished)
                .satisfiesExactlyInAnyOrder(
                        // test_without_constraint runs on all versions
                        ranWithNameAndVersion("test without constraint", "7.6.5"),
                        ranWithNameAndVersion("test without constraint", "8.0"),
                        ranWithNameAndVersion("test without constraint", "8.5"),
                        // test_greater_than_8_0 only runs on 8.5
                        ranWithNameAndVersion("test greater than 8 0", "8.5"),
                        // test_greater_than_7_6_5 runs on 8.0 and 8.5
                        ranWithNameAndVersion("test greater than 7 6 5", "8.0"),
                        ranWithNameAndVersion("test greater than 7 6 5", "8.5"));

        assertThat(skipped)
                .satisfiesExactlyInAnyOrder(
                        // test_greater_than_8_0 skipped for 7.6.5 and 8.0
                        skippedWithNameAndVersion("test greater than 8 0", "7.6.5"),
                        skippedWithNameAndVersion("test greater than 8 0", "8.0"),
                        // test_greater_than_7_6_5 skipped for 7.6.5
                        skippedWithNameAndVersion("test greater than 7 6 5", "7.6.5"),
                        // test_greater_than_9_0_no_match skipped for all versions
                        skippedWithNameAndVersion("test greater than 9 0 no match", "7.6.5"),
                        skippedWithNameAndVersion("test greater than 9 0 no match", "8.0"),
                        skippedWithNameAndVersion("test greater than 9 0 no match", "8.5"));
    }

    private static Consumer<Event> ranWithNameAndVersion(String displayNameContains, String gradleVersion) {
        return event -> {
            assertThat(event.getTestDescriptor().getDisplayName()).contains(displayNameContains);
            Assertions.assertThatRanWithCorrectGradleVersion(
                    WithGradleVersionsGreaterThanFixtureTest.class, event, gradleVersion, displayNameContains);
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
