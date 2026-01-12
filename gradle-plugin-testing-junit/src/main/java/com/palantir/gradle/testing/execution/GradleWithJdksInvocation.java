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

import java.util.concurrent.Callable;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

public record GradleWithJdksInvocation(GradleInvocation setupInvocation, Callable<GradleInvocation> tasksInvocation)
        implements GradleInvocation {

    private static final Logger logger = Logging.getLogger(GradleWithJdksInvocation.class);

    @Override
    public InvocationResult buildsSuccessfully() {
        setupJdkAutomanagement();
        try {
            return tasksInvocation.call().buildsSuccessfully();
        } catch (Exception e) {
            throw new RuntimeException("Failed to run the gradle invoker", e);
        }
    }

    @Override
    public InvocationResult buildsWithFailure() {
        setupJdkAutomanagement();
        try {
            return tasksInvocation.call().buildsWithFailure();
        } catch (Exception e) {
            throw new RuntimeException("Failed to run the gradle invoker", e);
        }
    }

    public void setupJdkAutomanagement() {
        try {
            setupInvocation.buildsSuccessfully();
        } catch (Exception e) {
            throw new GradleWithJdksInvocationFailure(e);
        }
    }
}
