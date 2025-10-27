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

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.palantir.gradle.testing.execution.GradleInvocation;
import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.execution.InvocationResult;
import com.palantir.gradle.testing.execution.TaskOutcome;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import com.palantir.gradle.testing.project.SubProject;
import org.junit.jupiter.api.Test;

@GradlePluginTests
class InvocationResultUsagesTest {
    @Test
    void a_failing_build_throws_an_error_when_it_was_expected_to_succeed(
            GradleInvoker gradle, RootProject rootProject) {

        rootProject.buildGradle().appendLine("throw new RuntimeException('oops')");

        GradleInvocation gradleInvocation = gradle.withArgs();

        assertThatExceptionOfType(RuntimeException.class).isThrownBy(gradleInvocation::buildsSuccessfully);
    }

    @Test
    void a_successful_build_throws_an_error_when_it_was_expected_to_fail(GradleInvoker gradle) {
        GradleInvocation gradleInvocation = gradle.withArgs();

        assertThatExceptionOfType(RuntimeException.class).isThrownBy(gradleInvocation::buildsWithFailure);
    }

    @Test
    void can_get_build_output_after_running_a_successful_build(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            println 'hello from build'
            """);

        InvocationResult result = gradle.withArgs().buildsSuccessfully();

        result.assertThat().output().contains("hello from build");
    }

    @Test
    void non_existent_tasks_are_returned_as_optional_empty(GradleInvoker gradle) {
        InvocationResult result = gradle.withArgs().buildsSuccessfully();

        result.assertThat().task(":i-dont-exist").isEmpty();
    }

    @Test
    void excluded_tasks_are_returned_as_optional_empty(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().appendLine("tasks.register('foo')");

        InvocationResult result = gradle.withArgs("foo", "-x", "foo").buildsSuccessfully();

        result.assertThat().task(":foo").isEmpty();
    }

    @Test
    void task_path_is_correct(GradleInvoker gradle, SubProject subProject) {
        subProject.buildGradle().append("""
            tasks.register('foo') {
                doLast {}
            }
            """);

        InvocationResult result = gradle.withArgs("foo").buildsSuccessfully();

        result.assertThat().task(":sub:foo").path().isEqualTo(":sub:foo");
    }

    @Test
    void can_get_info_about_a_successful_task(GradleInvoker gradle, SubProject subProject) {
        subProject.buildGradle().append("""
            tasks.register('foo') {
                // Needs a task action to not be up-to-date
                doLast {}
            }
            """);

        InvocationResult result = gradle.withArgs("foo").buildsSuccessfully();

        result.assertThat().task(":sub:foo").outcome().isEqualTo(TaskOutcome.SUCCESS);
    }

    @Test
    void can_get_info_about_a_failed_task(GradleInvoker gradle, SubProject subProject) {
        subProject.buildGradle().append("""
            tasks.register('foo') {
                doLast {
                    throw new RuntimeException('oops')
                }
            }
            """);

        InvocationResult result = gradle.withArgs("foo").buildsWithFailure();

        result.assertThat().task(":sub:foo").outcome().isEqualTo(TaskOutcome.FAILED);
    }

    @Test
    void can_get_info_about_an_up_to_date_task(GradleInvoker gradle, SubProject subProject) {
        subProject.buildGradle().append("""
            tasks.register('foo') {
                // No task actions -> up-to-date
            }
            """);

        InvocationResult result = gradle.withArgs("foo").buildsSuccessfully();

        result.assertThat().task(":sub:foo").outcome().isEqualTo(TaskOutcome.UP_TO_DATE);
    }

    @Test
    void can_get_info_about_a_skipped_task(GradleInvoker gradle, SubProject subProject) {
        subProject.buildGradle().append("""
            tasks.register('foo') {
                onlyIf { false }
            }
            """);

        InvocationResult result = gradle.withArgs("foo").buildsSuccessfully();

        result.assertThat().task(":sub:foo").outcome().isEqualTo(TaskOutcome.SKIPPED);
    }

    @Test
    void can_get_info_about_a_no_source_task(GradleInvoker gradle, SubProject subProject) {
        subProject.buildGradle().append("""
            tasks.register('foo', Copy) {}
            """);

        InvocationResult result = gradle.withArgs("foo").buildsSuccessfully();

        result.assertThat().task(":sub:foo").outcome().isEqualTo(TaskOutcome.NO_SOURCE);
    }
}
