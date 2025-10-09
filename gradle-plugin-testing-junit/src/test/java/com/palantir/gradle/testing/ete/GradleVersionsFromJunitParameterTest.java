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

import com.palantir.example.MultipleGradleVersionsFixtureTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Event;

final class GradleVersionsFromJunitParameterTest {
    @Test
    void runs_tests_with_gradle_versions_from_junit_parameter() {
        EngineExecutionResults executionResults = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(MultipleGradleVersionsFixtureTest.class))
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", "7.6.4,8.12.1")
                .execute();

        List<Event> finished = executionResults.testEvents().finished().stream().toList();

        assertThat(finished).hasSize(2);

        assertThatTestContainerDescriptorHasDisplayName(finished.get(0), "Gradle 7.6.4");
        assertThatTestContainerDescriptorHasDisplayName(finished.get(1), "Gradle 8.12.1");

        List<TestExecutionResult> testExecutionResults = finished.stream()
                .map(even -> even.getPayload(TestExecutionResult.class).get())
                .toList();

        assertThat(testExecutionResults).allSatisfy(result -> {
            assertThat(result.getStatus()).isEqualTo(Status.FAILED);
        });

        assertThatTestFailureExceptionMessageContains(testExecutionResults.get(0), "GradleVersion: 7.6.4");
        assertThatTestFailureExceptionMessageContains(testExecutionResults.get(1), "GradleVersion: 8.12.1");
    }

    private static void assertThatTestContainerDescriptorHasDisplayName(
            Event event, String containerDescriptorDisplayName) {
        assertThat(event.getTestDescriptor().getParent()).hasValueSatisfying(desc -> {
            assertThat(desc.getDisplayName()).isEqualTo(containerDescriptorDisplayName);
        });
    }

    private static void assertThatTestFailureExceptionMessageContains(
            TestExecutionResult testExecutionResult, String exceptionFragment) {
        assertThat(testExecutionResult.getThrowable()).hasValueSatisfying(throwable -> {
            assertThat(throwable).hasMessageContaining(exceptionFragment);
        });
    }
}
