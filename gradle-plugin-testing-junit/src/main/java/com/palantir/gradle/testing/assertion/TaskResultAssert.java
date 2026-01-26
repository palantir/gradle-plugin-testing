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

import com.palantir.gradle.testing.execution.TaskResult;
import java.util.Optional;
import org.assertj.core.api.AbstractOptionalAssert;

public final class TaskResultAssert extends AbstractOptionalAssert<TaskResultAssert, TaskResult> {

    private final String taskPath;

    TaskResultAssert(Optional<TaskResult> optional, String taskPath) {
        super(optional, TaskResultAssert.class);
        this.taskPath = taskPath;
    }

    private TaskResult requireTaskResult() {
        if (actual.isEmpty()) {
            failWithMessage("Expected to find a task result for task '%s' but there was none.".formatted(taskPath));
        }
        return actual.get();
    }

    /**
     * Returns an assertion object for the task outcome.
     *
     * @return a {@link TaskOutcomeAssert} for fluent assertion chaining
     */
    public TaskOutcomeAssert outcome() {
        return new TaskOutcomeAssert(requireTaskResult().outcome());
    }

    /**
     * Asserts that the task succeeded.
     * <p>
     * This is equivalent to {@code outcome().succeeded()}.
     *
     * @return a {@link TaskOutcomeAssert} for fluent assertion chaining
     */
    public TaskOutcomeAssert succeeded() {
        return outcome().succeeded();
    }

    /**
     * Asserts that the task failed.
     * <p>
     * This is equivalent to {@code outcome().failed()}.
     *
     * @return a {@link TaskOutcomeAssert} for fluent assertion chaining
     */
    public TaskOutcomeAssert failed() {
        return outcome().failed();
    }

    /**
     * Asserts that the task was up-to-date.
     * <p>
     * This is equivalent to {@code outcome().upToDate()}.
     *
     * @return a {@link TaskOutcomeAssert} for fluent assertion chaining
     */
    public TaskOutcomeAssert upToDate() {
        return outcome().upToDate();
    }

    /**
     * Asserts that the task was skipped.
     * <p>
     * This is equivalent to {@code outcome().skipped()}.
     *
     * @return a {@link TaskOutcomeAssert} for fluent assertion chaining
     */
    public TaskOutcomeAssert skipped() {
        return outcome().skipped();
    }

    /**
     * Asserts that the task result was retrieved from the build cache.
     * <p>
     * This is equivalent to {@code outcome().fromCache()}.
     *
     * @return a {@link TaskOutcomeAssert} for fluent assertion chaining
     */
    public TaskOutcomeAssert fromCache() {
        return outcome().fromCache();
    }

    /**
     * Asserts that the task had no source files to process.
     * <p>
     * This is equivalent to {@code outcome().noSource()}.
     *
     * @return a {@link TaskOutcomeAssert} for fluent assertion chaining
     */
    public TaskOutcomeAssert noSource() {
        return outcome().noSource();
    }

    /**
     * Asserts that the task was not on the task graph.
     *
     * @return this assertion object for method chaining
     */
    public TaskResultAssert notOnTaskGraph() {
        as("Task '%s' was found on task graph", taskPath).isEmpty();
        return this;
    }
}
