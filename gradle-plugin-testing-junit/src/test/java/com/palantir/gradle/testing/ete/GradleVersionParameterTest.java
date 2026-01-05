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

import com.palantir.example.GradleVersionAssumptionFixtureTest;
import com.palantir.example.GradleVersionParameterFixtureTest;
import com.palantir.example.GradleVersionWithAdditionalVersionsFixtureTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Event;

final class GradleVersionParameterTest {
    @Test
    void gradle_version_is_injected_as_test_parameter() {
        EngineExecutionResults executionResults = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(GradleVersionParameterFixtureTest.class))
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", "7.6.5,8.14.3")
                .configurationParameter("com.palantir.gradle.testing.configuration_cache_enabled", "false")
                .execute();

        List<Event> finished = executionResults.testEvents().finished().stream().toList();

        assertThat(finished).hasSize(2);

        assertThatTestReceivedGradleVersion(finished.get(0), "7.6.5");
        assertThatTestReceivedGradleVersion(finished.get(1), "8.14.3");
    }

    @Test
    void gradle_version_can_be_used_with_assumptions_to_skip_tests() {
        EngineExecutionResults executionResults = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(GradleVersionAssumptionFixtureTest.class))
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", "7.6.5,8.14.3")
                .configurationParameter("com.palantir.gradle.testing.configuration_cache_enabled", "false")
                .execute();

        List<Event> finished = executionResults.testEvents().finished().stream().toList();

        assertThat(finished).hasSize(2);

        // Gradle 7.6.5 should be skipped due to assumption
        assertThat(finished.get(0).getPayload(TestExecutionResult.class)).hasValueSatisfying(result -> {
            assertThat(result.getStatus()).isEqualTo(Status.ABORTED);
        });

        // Gradle 8.14.3 should run and fail (with our test exception)
        assertThat(finished.get(1).getPayload(TestExecutionResult.class)).hasValueSatisfying(result -> {
            assertThat(result.getStatus()).isEqualTo(Status.FAILED);
            Assertions.assertThatTestFailureExceptionMessageContains(result, "Test ran on: 8.14.3");
        });
    }

    @Test
    void gradle_version_works_with_additional_gradle_versions_annotation() {
        EngineExecutionResults executionResults = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(GradleVersionWithAdditionalVersionsFixtureTest.class))
                // Base version from config, 8.0 and 8.5 from @AdditionalGradleVersions
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", "7.6.5")
                .configurationParameter("com.palantir.gradle.testing.configuration_cache_enabled", "false")
                .execute();

        List<Event> finished = executionResults.testEvents().finished().stream().toList();

        // Should have 3 test runs: 7.6.5 from config + 8.0 and 8.5 from @AdditionalGradleVersions
        assertThat(finished).hasSize(3);

        // 7.6.5 should run (not skipped)
        assertThat(finished.get(0).getPayload(TestExecutionResult.class)).hasValueSatisfying(result -> {
            assertThat(result.getStatus()).isEqualTo(Status.FAILED);
            Assertions.assertThatTestFailureExceptionMessageContains(result, "Test ran on: 7.6.5");
        });

        // 8.0 should be skipped due to assumption
        assertThat(finished.get(1).getPayload(TestExecutionResult.class)).hasValueSatisfying(result -> {
            assertThat(result.getStatus()).isEqualTo(Status.ABORTED);
        });

        // 8.5 should run (not skipped)
        assertThat(finished.get(2).getPayload(TestExecutionResult.class)).hasValueSatisfying(result -> {
            assertThat(result.getStatus()).isEqualTo(Status.FAILED);
            Assertions.assertThatTestFailureExceptionMessageContains(result, "Test ran on: 8.5");
        });
    }

    private static void assertThatTestReceivedGradleVersion(Event event, String expectedVersion) {
        assertThat(event.getPayload(TestExecutionResult.class)).hasValueSatisfying(testExecutionResult -> {
            assertThat(testExecutionResult.getStatus()).isEqualTo(Status.FAILED);
            Assertions.assertThatTestFailureExceptionMessageContains(
                    testExecutionResult, "GradleVersion: " + expectedVersion);
        });
    }
}
