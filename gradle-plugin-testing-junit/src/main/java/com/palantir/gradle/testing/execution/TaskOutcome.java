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

public enum TaskOutcome {
    SUCCESS,
    FAILED,
    UP_TO_DATE,
    SKIPPED,
    FROM_CACHE,
    NO_SOURCE;

    static TaskOutcome fromGradleTaskOutcome(org.gradle.testkit.runner.TaskOutcome gradleTaskOutcome) {
        return switch (gradleTaskOutcome) {
            case SUCCESS -> SUCCESS;
            case FAILED -> FAILED;
            case UP_TO_DATE -> UP_TO_DATE;
            case SKIPPED -> SKIPPED;
            case FROM_CACHE -> FROM_CACHE;
            case NO_SOURCE -> NO_SOURCE;
            default -> throw new IllegalArgumentException("Unknown gradle task outcome: " + gradleTaskOutcome);
        };
    }
}
