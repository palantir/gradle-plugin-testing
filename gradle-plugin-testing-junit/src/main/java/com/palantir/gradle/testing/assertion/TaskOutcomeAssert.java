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

    public TaskOutcomeAssert wasSuccess() {
        if (actual != TaskOutcome.SUCCESS) {
            failWithMessage("Expected task outcome to be SUCCESS but was %s", actual);
        }
        return this;
    }

    public TaskOutcomeAssert wasFail() {
        if (actual != TaskOutcome.FAILED) {
            failWithMessage("Expected task outcome to be FAILED but was %s", actual);
        }
        return this;
    }

    public TaskOutcomeAssert wasUpToDate() {
        if (actual != TaskOutcome.UP_TO_DATE) {
            failWithMessage("Expected task outcome to be UP_TO_DATE but was %s", actual);
        }
        return this;
    }
}
