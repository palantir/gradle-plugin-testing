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
import org.assertj.core.api.AbstractObjectAssert;

public final class TaskOutcomeAssert extends AbstractObjectAssert<TaskOutcomeAssert, TaskOutcome> {

    TaskOutcomeAssert(TaskOutcome taskOutcome) {
        super(taskOutcome, TaskOutcomeAssert.class);
    }

    /**
     * Asserts that the task outcome is {@link TaskOutcome#SUCCESS}.
     *
     * @return this assertion object for method chaining
     */
    public TaskOutcomeAssert succeeded() {
        assertTaskOutcome(TaskOutcome.SUCCESS);
        return this;
    }

    /**
     * Asserts that the task outcome is {@link TaskOutcome#FAILED}.
     *
     * @return this assertion object for method chaining
     */
    public TaskOutcomeAssert failed() {
        assertTaskOutcome(TaskOutcome.FAILED);
        return this;
    }

    /**
     * Asserts that the task outcome is {@link TaskOutcome#UP_TO_DATE}.
     *
     * @return this assertion object for method chaining
     */
    public TaskOutcomeAssert upToDate() {
        assertTaskOutcome(TaskOutcome.UP_TO_DATE);
        return this;
    }

    private void assertTaskOutcome(TaskOutcome expected) {
        as("Expected task outcome to be %s but was %s", expected, actual).isEqualTo(expected);
    }
}
