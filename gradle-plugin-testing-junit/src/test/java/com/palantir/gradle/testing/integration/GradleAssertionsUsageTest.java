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

import static com.palantir.gradle.testing.assertion.GradlePluginTestAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.execution.InvocationResult;
import com.palantir.gradle.testing.execution.TaskOutcome;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@GradlePluginTests
class GradleAssertionsUsageTest {

    @Nested
    class TaskOutcomeAssertions {

        @Test
        void can_check_task_outcome(GradleInvoker gradle, RootProject rootProject) {
            rootProject.buildGradle().append("""
                tasks.register('foo') {
                    doLast {}
                }
                """);

            InvocationResult result = gradle.withArgs("foo").buildsSuccessfully();

            assertThat(result)
                    .task(":foo")
                    .as("Task should have SUCCESS outcome")
                    .outcome()
                    .isEqualTo(TaskOutcome.SUCCESS);
        }

        @Test
        void can_check_task_is_up_to_date(GradleInvoker gradle, RootProject rootProject) {
            rootProject.buildGradle().append("""
                tasks.register('foo') {
                    def outputFile = layout.projectDirectory.file('foo.txt')
                    outputs.file(outputFile)

                    doLast {
                        outputFile.asFile.text = 'hello'
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
        void assertion_fails_when_outcome_does_not_match(GradleInvoker gradle, RootProject rootProject) {
            rootProject.buildGradle().append("""
                tasks.register('foo') {
                    doLast {}
                }
                """);

            InvocationResult result = gradle.withArgs("foo").buildsSuccessfully();

            assertThatExceptionOfType(AssertionError.class)
                    .isThrownBy(() -> result.assertThat()
                            .task(":foo")
                            .as("Task outcome should match")
                            .outcome()
                            .isEqualTo(TaskOutcome.UP_TO_DATE))
                    .withMessageContaining("UP_TO_DATE")
                    .withMessageContaining("SUCCESS");
        }

        @Test
        void assertion_fails_when_task_is_not_present(GradleInvoker gradle) {
            InvocationResult result = gradle.withArgs().buildsSuccessfully();

            assertThatExceptionOfType(AssertionError.class)
                    .isThrownBy(() -> result.assertThat()
                            .task(":nonexistent")
                            .as("Task should not be present")
                            .outcome())
                    .withMessageContaining("Expected to find a task result for task ':nonexistent' but there was none.");
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
        void notOnTaskGraph_fails_when_task_exists(GradleInvoker gradle, RootProject rootProject) {
            rootProject.buildGradle().append("""
                tasks.register('foo') {
                    doLast {}
                }
                """);

            InvocationResult result = gradle.withArgs("foo").buildsSuccessfully();

            assertThatExceptionOfType(AssertionError.class)
                    .isThrownBy(() -> result.assertThat().task(":foo").notOnTaskGraph())
                    .withMessageContaining("Task ':foo' was found on task graph");
        }
    }

    @Nested
    class SucceededAssertions {

        @Test
        void can_check_task_succeeded(GradleInvoker gradle, RootProject rootProject) {
            rootProject.buildGradle().append("""
                tasks.register('foo') {
                    doLast {}
                }
                """);

            InvocationResult result = gradle.withArgs("foo").buildsSuccessfully();

            assertThat(result).task(":foo").succeeded();
        }

        @Test
        void can_check_task_succeeded_via_outcome(GradleInvoker gradle, RootProject rootProject) {
            rootProject.buildGradle().append("""
                tasks.register('foo') {
                    doLast {}
                }
                """);

            InvocationResult result = gradle.withArgs("foo").buildsSuccessfully();

            assertThat(result).task(":foo").outcome().succeeded();
        }

        @Test
        void succeeded_fails_when_task_not_present(GradleInvoker gradle) {
            InvocationResult result = gradle.withArgs().buildsSuccessfully();

            assertThatExceptionOfType(AssertionError.class)
                    .isThrownBy(() -> result.assertThat().task(":nonexistent").succeeded())
                    .withMessageContaining("Expected to find a task result for task ':nonexistent' but there was none.");
        }

        @Test
        void succeeded_fails_when_outcome_is_different(GradleInvoker gradle, RootProject rootProject) {
            rootProject.buildGradle().append("""
                tasks.register('foo') {
                    def outputFile = layout.projectDirectory.file('foo.txt')
                    outputs.file(outputFile)

                    doLast {
                        outputFile.asFile.text = 'hello'
                    }
                }
                """);

            gradle.withArgs("foo").buildsSuccessfully();
            InvocationResult secondRun = gradle.withArgs("foo").buildsSuccessfully();

            assertThatExceptionOfType(AssertionError.class)
                    .isThrownBy(() -> secondRun.assertThat().task(":foo").succeeded())
                    .withMessageContaining("Expected task outcome to be SUCCESS but was UP_TO_DATE");
        }
    }

    @Nested
    class FailedAssertions {

        @Test
        void can_check_task_failed(GradleInvoker gradle, RootProject rootProject) {
            rootProject.buildGradle().append("""
                tasks.register('foo') {
                    doLast {
                        try {
                            throw new IOException("Some exception")
                        } catch (Exception e) {
                            throw new RuntimeException('intentional failure', e)
                        }
                    }
                }
                """);

            InvocationResult result = gradle.withArgs("foo").buildsWithFailure();

            result.assertThat().task(":foo").failed();
            assertThat(result).output().contains("Caused by: java.io.IOException: Some exception");
        }

        @Test
        void can_check_task_failed_via_outcome(GradleInvoker gradle, RootProject rootProject) {
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
        void failed_fails_when_outcome_is_different(GradleInvoker gradle, RootProject rootProject) {
            rootProject.buildGradle().append("""
                tasks.register('foo') {
                    doLast {}
                }
                """);

            InvocationResult result = gradle.withArgs("foo").buildsSuccessfully();

            assertThatExceptionOfType(AssertionError.class)
                    .isThrownBy(() -> result.assertThat().task(":foo").failed())
                    .withMessageContaining("Expected task outcome to be FAILED but was SUCCESS");
        }
    }

    @Nested
    class UpToDateAssertions {

        @Test
        void can_check_task_upToDate(GradleInvoker gradle, RootProject rootProject) {
            rootProject.buildGradle().append("""
                tasks.register('foo') {
                    def outputFile = layout.projectDirectory.file('foo.txt')
                    outputs.file(outputFile)

                    doLast {
                        outputFile.asFile.text = 'hello'
                    }
                }
                """);

            gradle.withArgs("foo").buildsSuccessfully();
            InvocationResult secondRun = gradle.withArgs("foo").buildsSuccessfully();

            secondRun.assertThat().task(":foo").upToDate();
        }

        @Test
        void can_check_task_upToDate_via_outcome(GradleInvoker gradle, RootProject rootProject) {
            rootProject.buildGradle().append("""
                tasks.register('foo') {
                    def outputFile = layout.projectDirectory.file('foo.txt')
                    outputs.file(outputFile)

                    doLast {
                        outputFile.asFile.text = 'hello'
                    }
                }
                """);

            gradle.withArgs("foo").buildsSuccessfully();
            InvocationResult secondRun = gradle.withArgs("foo").buildsSuccessfully();

            secondRun.assertThat().task(":foo").outcome().upToDate();
        }

        @Test
        void upToDate_fails_when_outcome_is_different(GradleInvoker gradle, RootProject rootProject) {
            rootProject.buildGradle().append("""
                tasks.register('foo') {
                    doLast {}
                }
                """);

            InvocationResult result = gradle.withArgs("foo").buildsSuccessfully();

            assertThatExceptionOfType(AssertionError.class)
                    .isThrownBy(() -> result.assertThat().task(":foo").upToDate())
                    .withMessageContaining("Expected task outcome to be UP_TO_DATE but was SUCCESS");
        }
    }

    @Nested
    class OutputAssertions {

        @Test
        void can_check_output(GradleInvoker gradle, RootProject rootProject) {
            rootProject.buildGradle().append("""
                println 'hello from build'
                """);

            InvocationResult result = gradle.withArgs().buildsSuccessfully();

            assertThat(result)
                    .output()
                    .as("Build output should contain expected message")
                    .contains("hello from build");
        }
    }

    @Nested
    class FluentApiUsage {

        @Test
        void can_use_satisfies_and_extracting_on_invocation_result(GradleInvoker gradle, RootProject rootProject) {
            rootProject.buildGradle().append("""
                tasks.register('foo') {
                    doLast {}
                }
                """);

            InvocationResult result = gradle.withArgs("foo").buildsSuccessfully();

            result.assertThat()
                    .satisfies(invocationAssert -> {
                        invocationAssert.assertThat().task(":foo").succeeded();
                        invocationAssert.assertThat().output().contains("BUILD SUCCESSFUL");
                    })
                    .extracting(InvocationResult::output)
                    .asString()
                    .contains("foo");
        }
    }
}
