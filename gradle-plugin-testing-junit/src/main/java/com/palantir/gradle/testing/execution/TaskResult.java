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

import org.gradle.testkit.runner.BuildTask;

public final class TaskResult {
    private final BuildTask buildTask;

    TaskResult(BuildTask buildTask) {
        this.buildTask = buildTask;
    }

    public String path() {
        return buildTask.getPath();
    }

    public TaskOutcome outcome() {
        return TaskOutcome.fromGradleTaskOutcome(buildTask.getOutcome());
    }
}
