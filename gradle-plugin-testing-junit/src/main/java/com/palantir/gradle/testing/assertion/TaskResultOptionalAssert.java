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

import com.palantir.gradle.testing.execution.TaskOutcome;
import com.palantir.gradle.testing.execution.TaskResult;
import java.util.Optional;
import org.assertj.core.api.OptionalAssert;

public final class TaskResultOptionalAssert extends OptionalAssert<TaskResult> {

    TaskResultOptionalAssert(Optional<TaskResult> optional) {
        super(optional);
    }

    @Override
    public TaskResultOptionalAssert as(String description, Object... args) {
        return (TaskResultOptionalAssert) super.as(description, args);
    }

    public TaskResultOptionalAssert hasOutcome(TaskOutcome expected) {
        isPresent();
        TaskOutcome actualOutcome = actual.get().outcome();
        if (actualOutcome != expected) {
            failWithMessage("Expected task outcome to be <%s> but was <%s>", expected, actualOutcome);
        }
        return this;
    }

    public TaskResultOptionalAssert hasPath(String expectedPath) {
        isPresent();
        TaskResult taskResult = actual.get();
        if (!taskResult.path().equals(expectedPath)) {
            failWithMessage("Expected task path to be <%s> but was <%s>", expectedPath, taskResult.path());
        }
        return this;
    }
}
