/*
 * (c) Copyright 2026 Palantir Technologies Inc. All rights reserved.
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

import com.palantir.example.IncompatibleDecoratorsFixtureTest;
import com.palantir.gradle.plugintesting.GradleDistributionBaseUrl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Event;

final class IncompatibleDecoratorsTest {

    @Test
    void fails_when_decorator_has_incompatible_annotations_set() {
        EngineExecutionResults executionResults = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(IncompatibleDecoratorsFixtureTest.class))
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", "8.14.3")
                .configurationParameter("com.palantir.gradle.testing.configuration_cache_enabled", "false")
                .configurationParameter(
                        GradleDistributionBaseUrl.GRADLE_DISTRIBUTION_BASE_URL_SYSTEM_PROPERTY,
                        GradleDistributionBaseUrl.DEFAULT_BASE_URL)
                .execute();

        List<Event> finished = executionResults.testEvents().finished().stream().toList();

        assertThat(finished).hasSize(1);

        assertThat(finished.get(0).getPayload()).hasValueSatisfying(optionalResult -> {
            assertThat(optionalResult).isInstanceOfSatisfying(TestExecutionResult.class, testExecutionResult -> {
                assertThat(testExecutionResult.getStatus()).isEqualTo(Status.FAILED);

                Assertions.assertThatTestFailureExceptionMessageContains(
                        testExecutionResult,
                        "Type mismatch: Decorator SomeDecorator expects annotations of type"
                                + " DisabledConfigurationCache, but received incompatible annotation types:"
                                + " [WithWrongDecoratorAnnotation]");
            });
        });
    }
}
