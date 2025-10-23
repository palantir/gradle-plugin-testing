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

package com.palantir.gradle.testing.assertions;

import static com.palantir.gradle.testing.assertions.GradleAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.execution.InvocationResult;
import com.palantir.gradle.testing.execution.TaskOutcome;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.Test;

@GradlePluginTests
class GradleAssertionsTest {

    @Test
    void can_use_fluent_assertions_for_task_outcome_not_in(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            tasks.register('foo') {
                outputs.file('foo.txt')
                doLast {
                    file('foo.txt').text = 'hello'
                }
            }
            """);

        InvocationResult firstRun = gradle.withArgs("foo").buildsSuccessfully();
        InvocationResult secondRun = gradle.withArgs("foo").buildsSuccessfully();

        assertThat(firstRun)
                .task(":foo")
                .as("First run should execute the task")
                .hasOutcome()
                .as("First run task outcome should not be cached")
                .isNotIn(TaskOutcome.UP_TO_DATE, TaskOutcome.FROM_CACHE);

        assertThat(secondRun)
                .task(":foo")
                .as("Second run should have task cached")
                .hasOutcome()
                .as("Second run task outcome should be cached")
                .isIn(TaskOutcome.UP_TO_DATE, TaskOutcome.FROM_CACHE);
    }

    @Test
    void can_use_fluent_assertions_for_task_path(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            tasks.register('foo') {
                doLast {}
            }
            """);

        InvocationResult result = gradle.withArgs("foo").buildsSuccessfully();

        assertThat(result).task(":foo").as("Task should have correct path").hasPath(":foo");
    }

    @Test
    void can_use_fluent_assertions_for_output(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            println 'hello from build'
            """);

        InvocationResult result = gradle.withArgs().buildsSuccessfully();

        assertThat(result).as("Build output should contain expected message").hasOutput("hello from build");
    }

    @Test
    void fluent_assertions_fail_when_outcome_is_in_excluded_list(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            tasks.register('foo') {
                // No action - will be UP_TO_DATE
            }
            """);

        InvocationResult result = gradle.withArgs("foo").buildsSuccessfully();

        assertThatThrownBy(() -> assertThat(result)
                        .task(":foo")
                        .as("Task should be present")
                        .hasOutcome()
                        .as("Task outcome validation")
                        .isNotIn(TaskOutcome.UP_TO_DATE, TaskOutcome.FROM_CACHE))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected task outcome not to be in");
    }

    @Test
    void fluent_assertions_fail_when_outcome_is_not_in_expected_list(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            tasks.register('foo') {
                doLast {}
            }
            """);

        InvocationResult result = gradle.withArgs("foo").buildsSuccessfully();

        assertThatThrownBy(() -> assertThat(result)
                        .task(":foo")
                        .as("Task should be present")
                        .hasOutcome()
                        .as("Task outcome validation")
                        .isIn(TaskOutcome.UP_TO_DATE, TaskOutcome.FROM_CACHE))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected task outcome to be in");
    }

    @Test
    void fluent_assertions_fail_when_task_is_not_present(GradleInvoker gradle) {
        InvocationResult result = gradle.withArgs().buildsSuccessfully();

        assertThatThrownBy(() -> assertThat(result)
                        .task(":nonexistent")
                        .as("Task should not be present")
                        .hasOutcome())
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void can_check_task_is_empty(GradleInvoker gradle) {
        InvocationResult result = gradle.withArgs().buildsSuccessfully();

        assertThat(result).task(":nonexistent").as("Non-existent task should be empty").isEmpty();
    }
}
