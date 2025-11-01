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

import com.palantir.example.ProjectDirsAreMadeCorrectlyInBuildDirFixtureTest;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

final class ProjectDirsAreMadeCorrectlyInBuildDirTest {
    @Test
    void runs_tests_with_gradle_versions_from_junit_parameter() {
        EngineExecutionResults executionResults = EngineTestKit.engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(ProjectDirsAreMadeCorrectlyInBuildDirFixtureTest.class))
                .configurationParameter("com.palantir.gradle.testing.gradle_versions_to_test", "7.6.5,8.14.3")
                .execute();

        executionResults.testEvents().finished().assertThatEvents().allSatisfy(event -> {
            assertThat(event.getPayload()).hasValueSatisfying(optionalResult -> {
                assertThat(optionalResult).isInstanceOfSatisfying(TestExecutionResult.class, testExecutionResult -> {
                    assertThat(testExecutionResult.getStatus()).isEqualTo(Status.SUCCESSFUL);
                });
            });
        });

        Path rootTestDir = Path.of(
                "build/gradle-plugin-testing", ProjectDirsAreMadeCorrectlyInBuildDirFixtureTest.class.getSimpleName());

        Stream.of(
                        "regular test/7.6.5",
                        "regular test/8.14.3",
                        "parameterized test/1_ foo/7.6.5",
                        "parameterized test/2_ bar/7.6.5",
                        "parameterized test/1_ foo/8.14.3",
                        "parameterized test/2_ bar/8.14.3",
                        "NestedClass/nested parameterized test/1_ foo/7.6.5",
                        "NestedClass/nested parameterized test/2_ bar/7.6.5",
                        "NestedClass/nested parameterized test/1_ foo/8.14.3",
                        "NestedClass/nested parameterized test/2_ bar/8.14.3",
                        "NestedClass/DoublyNestedClass/doubly nested parameterized test/1_ foo/7.6.5",
                        "NestedClass/DoublyNestedClass/doubly nested parameterized test/2_ bar/7.6.5",
                        "NestedClass/DoublyNestedClass/doubly nested parameterized test/1_ foo/8.14.3",
                        "NestedClass/DoublyNestedClass/doubly nested parameterized test/2_ bar/8.14.3")
                .forEach(path -> assertThat(rootTestDir.resolve(path).resolve("settings.gradle"))
                        .exists());
    }
}
