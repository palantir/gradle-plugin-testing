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
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;

public final class InvocationResultAssert {

    private final InvocationResult invocationResult;

    public InvocationResultAssert(InvocationResult invocationResult) {
        this.invocationResult = invocationResult;
    }

    /**
     * Returns an assertion object for fluent task assertion chaining.
     * <p>
     * Usage:
     * <pre>
     * invocationResult.assertThat().task(":myTask")
     *     .hasOutcome()
     *     .isNotIn(TaskOutcome.UP_TO_DATE, TaskOutcome.FROM_CACHE);
     * </pre>
     */
    public TaskResultOptionalAssert task(String taskPath) {
        return new TaskResultOptionalAssert(invocationResult.task(taskPath));
    }

    /**
     * Returns an assertion object for fluent output assertion chaining.
     * <p>
     * Usage:
     * <pre>
     * invocationResult.assertThat().output().contains("BUILD SUCCESSFUL");
     * </pre>
     */
    public AbstractStringAssert<?> output() {
        return Assertions.assertThat(invocationResult.output());
    }
}
