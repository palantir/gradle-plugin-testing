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

package com.palantir.gradle.testing.assertion;

import com.palantir.gradle.testing.execution.InvocationResult;
import com.palantir.gradle.testing.execution.TaskOutcome;

/**
 * Entry point for AssertJ-style assertions for Gradle plugin testing.
 * <p>
 * Import this class statically to use fluent assertions in your tests:
 * <pre>
 * import static com.palantir.gradle.testing.assertion.GradlePluginTestAssertions.assertThat;
 *
 * assertThat(invocationResult).task(":myTask").succeeded();
 * assertThat(taskOutcome).succeeded();
 * </pre>
 */
public final class GradlePluginTestAssertions {

    /**
     * Creates an assertion for an {@link InvocationResult}.
     *
     * @param invocationResult the invocation result to assert on
     * @return an {@link InvocationResultAssert} for fluent assertion chaining
     */
    public static InvocationResultAssert assertThat(InvocationResult invocationResult) {
        return new InvocationResultAssert(invocationResult);
    }

    /**
     * Creates an assertion for a {@link TaskOutcome}.
     *
     * @param taskOutcome the task outcome to assert on
     * @return a {@link TaskOutcomeAssert} for fluent assertion chaining
     */
    public static TaskOutcomeAssert assertThat(TaskOutcome taskOutcome) {
        return new TaskOutcomeAssert(taskOutcome);
    }

    private GradlePluginTestAssertions() {}
}
