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

package com.palantir.gradle.testing.integration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.execution.InvocationResult;
import com.palantir.gradle.testing.execution.TaskOutcome;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.Test;

@GradlePluginTests
class GradleAssertionsUsageTest {

    @Test
    void can_check_task_outcome(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            tasks.register('foo') {
                doLast {}
            }
            """);

        InvocationResult result = gradle.withArgs("foo").buildsSuccessfully();

        result.assertThat()
                .task(":foo")
                .as("Task should have SUCCESS outcome")
                .outcome()
                .isEqualTo(TaskOutcome.SUCCESS);
    }

    @Test
    void can_check_task_is_up_to_date(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            tasks.register('foo') {
                outputs.file('foo.txt')
                doLast {
                    file('foo.txt').text = 'hello'
                }
            }
            """);

        gradle.withArgs("foo").buildsSuccessfully();
        InvocationResult secondRun = gradle.withArgs("foo").buildsSuccessfully();

        secondRun
                .assertThat()
                .task(":foo")
                .as("Second run should be up to date")
                .outcome()
                .isEqualTo(TaskOutcome.UP_TO_DATE);
    }

    @Test
    void can_check_output(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            println 'hello from build'
            """);

        InvocationResult result = gradle.withArgs().buildsSuccessfully();

        result.assertThat()
                .output()
                .as("Build output should contain expected message")
                .contains("hello from build");
    }

    @Test
    void assertion_fails_when_outcome_does_not_match(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            tasks.register('foo') {
                doLast {}
            }
            """);

        InvocationResult result = gradle.withArgs("foo").buildsSuccessfully();

        assertThatThrownBy(() -> result.assertThat()
                        .task(":foo")
                        .as("Task outcome should match")
                        .outcome()
                        .isEqualTo(TaskOutcome.UP_TO_DATE))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("UP_TO_DATE")
                .hasMessageContaining("SUCCESS");
    }

    @Test
    void assertion_fails_when_task_is_not_present(GradleInvoker gradle) {
        InvocationResult result = gradle.withArgs().buildsSuccessfully();

        assertThatThrownBy(() -> result.assertThat()
                        .task(":nonexistent")
                        .as("Task should not be present")
                        .outcome())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected to find a task result for task ':nonexistent' but there was none.");
    }

    @Test
    void can_check_task_is_empty(GradleInvoker gradle) {
        InvocationResult result = gradle.withArgs().buildsSuccessfully();

        result.assertThat()
                .task(":nonexistent")
                .as("Non-existent task should be empty")
                .isEmpty();
    }
}
