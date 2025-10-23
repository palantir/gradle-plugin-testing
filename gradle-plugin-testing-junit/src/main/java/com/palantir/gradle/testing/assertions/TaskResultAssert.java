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

import com.palantir.gradle.testing.execution.TaskResult;
import org.assertj.core.api.AbstractObjectAssert;

public final class TaskResultAssert extends AbstractObjectAssert<TaskResultAssert, TaskResult> {

    TaskResultAssert(TaskResult taskResult) {
        super(taskResult, TaskResultAssert.class);
    }

    public TaskOutcomeAssert hasOutcome() {
        isNotNull();
        return new TaskOutcomeAssert(actual.outcome());
    }

    public TaskResultAssert hasPath(String expectedPath) {
        isNotNull();
        if (!actual.path().equals(expectedPath)) {
            failWithMessage("Expected task path to be <%s> but was <%s>", expectedPath, actual.path());
        }
        return this;
    }
}
