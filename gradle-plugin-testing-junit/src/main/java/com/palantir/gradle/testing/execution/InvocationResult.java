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

package com.palantir.gradle.testing.execution;

import java.util.Optional;
import org.gradle.testkit.runner.BuildResult;

public final class InvocationResult {
    private final BuildResult buildResult;

    InvocationResult(BuildResult buildResult) {
        this.buildResult = buildResult;
    }

    public String output() {
        return buildResult.getOutput();
    }

    public Optional<TaskResult> task(String taskPath) {
        return Optional.ofNullable(buildResult.task(taskPath)).map(TaskResult::new);
    }

    /**
     * Returns an assertion object for fluent task assertion chaining.
     * <p>
     * Usage:
     * <pre>
     * invocationResult.assertTask(":myTask")
     *     .hasOutcome()
     *     .isNotIn(TaskOutcome.UP_TO_DATE, TaskOutcome.FROM_CACHE);
     * </pre>
     */
    public InvocationResultAssert.TaskResultOptionalAssert assertTask(String taskPath) {
        return new InvocationResultAssert.TaskResultOptionalAssert(task(taskPath));
    }

    /**
     * Returns an assertion object for fluent output assertion chaining.
     * <p>
     * Usage:
     * <pre>
     * invocationResult.assertOutput().contains("BUILD SUCCESSFUL");
     * </pre>
     */
    public org.assertj.core.api.AbstractStringAssert<?> assertOutput() {
        return org.assertj.core.api.Assertions.assertThat(output());
    }
}
