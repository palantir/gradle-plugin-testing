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

import com.palantir.example.ConfigurationCacheFixtureTest;
import com.palantir.example.DisabledConfigurationCacheFixtureTest;
import com.palantir.example.DisabledConfigurationCacheFixtureTestBefore;
import com.palantir.gradle.plugintesting.GradleDistributionBaseUrl;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Event;

public class ConfigurationCacheParameterTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void runs_tests_with_configuration_cache_value(boolean isConfigurationCacheEnabled) {
        EngineExecutionResults executionResults = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(ConfigurationCacheFixtureTest.class))
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", "8.14.3")
                .configurationParameter(
                        "com.palantir.gradle.testing.configuration_cache_enabled",
                        String.valueOf(isConfigurationCacheEnabled))
                .configurationParameter(
                        GradleDistributionBaseUrl.GRADLE_DISTRIBUTION_BASE_URL_SYSTEM_PROPERTY,
                        GradleDistributionBaseUrl.DEFAULT_BASE_URL)
                .execute();

        List<Event> finished = executionResults.testEvents().finished().stream().toList();

        assertThat(finished).hasSize(1);

        assertThat(finished.get(0).getPayload(TestExecutionResult.class)).hasValueSatisfying(testExecutionResult -> {
            assertThat(testExecutionResult.getStatus()).isEqualTo(Status.FAILED);

            Assertions.assertThatTestFailureExceptionMessageContains(
                    testExecutionResult,
                    String.format("isConfigurationCacheRequested=%s", isConfigurationCacheEnabled));
        });
    }

    @ParameterizedTest
    @ValueSource(
            classes = {DisabledConfigurationCacheFixtureTestBefore.class, DisabledConfigurationCacheFixtureTest.class})
    void runs_tests_with_disabled_configuration_cache(Class<?> clazz) {
        EngineExecutionResults executionResults = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(clazz))
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", "8.14.3")
                // we require a value for this parameter to be set. Setting it to `invalid` to make sure the value will
                // always be overridden to false due to the `@DisabledConfigurationCache` annotation.
                .configurationParameter("com.palantir.gradle.testing.configuration_cache_enabled", "invalid")
                .configurationParameter(
                        GradleDistributionBaseUrl.GRADLE_DISTRIBUTION_BASE_URL_SYSTEM_PROPERTY,
                        GradleDistributionBaseUrl.DEFAULT_BASE_URL)
                .execute();

        List<Event> finished = executionResults.testEvents().finished().stream().toList();

        assertThat(finished).hasSize(1);

        assertThat(finished.get(0).getPayload(TestExecutionResult.class)).hasValueSatisfying(testExecutionResult -> {
            assertThat(testExecutionResult.getStatus()).isEqualTo(Status.FAILED);

            Assertions.assertThatTestFailureExceptionMessageContains(
                    testExecutionResult, "isConfigurationCacheRequested=false");
        });
    }
}
