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
import org.assertj.core.api.OptionalAssert;

public final class TaskResultAssert extends OptionalAssert<TaskResult> {

    private final String taskPath;

    TaskResultAssert(Optional<TaskResult> optional, String taskPath) {
        super(optional);
        this.taskPath = taskPath;
    }

    @Override
    public TaskResultAssert as(String description, Object... args) {
        return (TaskResultAssert) super.as(description, args);
    }

    private TaskResult requireTaskResult() {
        if (actual.isEmpty()) {
            failWithMessage("Expected to find a task result for task '%s' but there was none.".formatted(taskPath));
        }
        return actual.get();
    }

    public TaskOutcomeAssert outcome() {
        return new TaskOutcomeAssert(requireTaskResult().outcome());
    }

    public TaskOutcomeAssert succeeded() {
        return outcome().succeeded();
    }

    public TaskOutcomeAssert failed() {
        return outcome().failed();
    }

    public TaskOutcomeAssert upToDate() {
        return outcome().upToDate();
    }

    public TaskResultAssert notOnTaskGraph() {
        as("Task '%s' was found on task graph", taskPath).isEmpty();
        return this;
    }
}
