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
import java.util.Arrays;
import java.util.stream.Collectors;
import org.assertj.core.api.AbstractObjectAssert;

public final class TaskOutcomeAssert extends AbstractObjectAssert<TaskOutcomeAssert, TaskOutcome> {

    public TaskOutcomeAssert(TaskOutcome taskOutcome) {
        super(taskOutcome, TaskOutcomeAssert.class);
    }

    public TaskOutcomeAssert isIn(TaskOutcome... expectedOutcomes) {
        isNotNull();
        if (!contains(expectedOutcomes)) {
            failWithMessage("Expected task outcome to be in %s but was <%s>", formatOutcomes(expectedOutcomes), actual);
        }
        return this;
    }

    public TaskOutcomeAssert isNotIn(TaskOutcome... excludedOutcomes) {
        isNotNull();
        if (contains(excludedOutcomes)) {
            failWithMessage(
                    "Expected task outcome not to be in %s but was <%s>", formatOutcomes(excludedOutcomes), actual);
        }
        return this;
    }

    private boolean contains(TaskOutcome[] outcomes) {
        return Arrays.stream(outcomes).anyMatch(outcome -> outcome == actual);
    }

    private static String formatOutcomes(TaskOutcome[] outcomes) {
        return Arrays.stream(outcomes).map(Enum::name).collect(Collectors.joining(", ", "[", "]"));
    }
}
