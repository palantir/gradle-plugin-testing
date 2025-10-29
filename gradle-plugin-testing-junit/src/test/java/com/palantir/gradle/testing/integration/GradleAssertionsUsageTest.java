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
    void can_check_task_is_notOnTaskGraph(GradleInvoker gradle) {
        InvocationResult result = gradle.withArgs().buildsSuccessfully();

        result.assertThat()
                .task(":nonexistent")
                .as("Non-existent task should be empty")
                .notOnTaskGraph();
    }

    @Test
    void can_check_task_wasSuccess(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            tasks.register('foo') {
                doLast {}
            }
            """);

        InvocationResult result = gradle.withArgs("foo").buildsSuccessfully();

        result.assertThat().task(":foo").succeeded();
    }

    @Test
    void can_check_task_wasSuccess_via_outcome(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            tasks.register('foo') {
                doLast {}
            }
            """);

        InvocationResult result = gradle.withArgs("foo").buildsSuccessfully();

        result.assertThat().task(":foo").outcome().succeeded();
    }

    @Test
    void can_check_task_wasFail(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            tasks.register('foo') {
                doLast {
                    throw new RuntimeException('intentional failure')
                }
            }
            """);

        InvocationResult result = gradle.withArgs("foo").buildsWithFailure();

        result.assertThat().task(":foo").failed();
    }

    @Test
    void can_check_task_wasFail_via_outcome(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            tasks.register('foo') {
                doLast {
                    throw new RuntimeException('intentional failure')
                }
            }
            """);

        InvocationResult result = gradle.withArgs("foo").buildsWithFailure();

        result.assertThat().task(":foo").outcome().failed();
    }

    @Test
    void can_check_task_wasUpToDate(GradleInvoker gradle, RootProject rootProject) {
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

        secondRun.assertThat().task(":foo").upToDate();
    }

    @Test
    void can_check_task_wasUpToDate_via_outcome(GradleInvoker gradle, RootProject rootProject) {
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

        secondRun.assertThat().task(":foo").outcome().upToDate();
    }

    @Test
    void wasSuccess_fails_when_task_not_present(GradleInvoker gradle) {
        InvocationResult result = gradle.withArgs().buildsSuccessfully();

        assertThatThrownBy(() -> result.assertThat().task(":nonexistent").succeeded())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected to find a task result for task ':nonexistent' but there was none.");
    }

    @Test
    void wasSuccess_fails_when_outcome_is_different(GradleInvoker gradle, RootProject rootProject) {
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

        assertThatThrownBy(() -> secondRun.assertThat().task(":foo").succeeded())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected task outcome to be SUCCESS but was UP_TO_DATE");
    }

    @Test
    void wasFail_fails_when_outcome_is_different(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            tasks.register('foo') {
                doLast {}
            }
            """);

        InvocationResult result = gradle.withArgs("foo").buildsSuccessfully();

        assertThatThrownBy(() -> result.assertThat().task(":foo").failed())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected task outcome to be FAILED but was SUCCESS");
    }

    @Test
    void wasUpToDate_fails_when_outcome_is_different(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            tasks.register('foo') {
                doLast {}
            }
            """);

        InvocationResult result = gradle.withArgs("foo").buildsSuccessfully();

        assertThatThrownBy(() -> result.assertThat().task(":foo").upToDate())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected task outcome to be UP_TO_DATE but was SUCCESS");
    }

    @Test
    void notOnTaskGraph_fails_when_task_exists(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            tasks.register('foo') {
                doLast {}
            }
            """);

        InvocationResult result = gradle.withArgs("foo").buildsSuccessfully();

        assertThatThrownBy(() -> result.assertThat().task(":foo").notOnTaskGraph())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Task ':foo' was found on task graph");
    }
}
