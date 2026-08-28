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

import com.palantir.example.GradleVersionsFromJunitParameterFixtureTest;
import com.palantir.gradle.plugintesting.GradleDistributionBaseUrl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Event;

final class GradleVersionsFromJunitParameterTest {
    @Test
    void runs_tests_with_gradle_versions_from_junit_parameter() {
        EngineExecutionResults executionResults = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(GradleVersionsFromJunitParameterFixtureTest.class))
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", "8.14.3,9.3.1")
                .configurationParameter("com.palantir.gradle.testing.configuration_cache_enabled", "false")
                .configurationParameter(
                        GradleDistributionBaseUrl.GRADLE_DISTRIBUTION_BASE_URL_SYSTEM_PROPERTY,
                        GradleDistributionBaseUrl.DEFAULT_BASE_URL)
                .execute();

        List<Event> finished = executionResults.testEvents().finished().stream().toList();

        assertThat(finished).hasSize(2);

        Assertions.assertThatRanWithCorrectGradleVersion(
                GradleVersionsFromJunitParameterFixtureTest.class, finished.get(0), "8.14.3");
        Assertions.assertThatRanWithCorrectGradleVersion(
                GradleVersionsFromJunitParameterFixtureTest.class, finished.get(1), "9.3.1");
    }
}
