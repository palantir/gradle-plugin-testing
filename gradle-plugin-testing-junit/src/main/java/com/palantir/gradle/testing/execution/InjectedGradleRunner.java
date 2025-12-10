/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.gradle.testing.execution;

public class InjectedGradleRunner extends AbstractGradleRunner {
    private final NamedToolingApiGradleExecutor assignedExecutor;

    /**
     * Constructor for pre-assigned executor approach (uses a specific executor assigned by JUnit).
     */
    public InjectedGradleRunner(NamedToolingApiGradleExecutor assignedExecutor) {
        super();
        this.assignedExecutor = assignedExecutor;
    }

    protected NamedToolingApiGradleExecutor getNextExecutor() {
        System.err.println("Using pre-assigned executor: " + assignedExecutor);
        return assignedExecutor;
    }
}
