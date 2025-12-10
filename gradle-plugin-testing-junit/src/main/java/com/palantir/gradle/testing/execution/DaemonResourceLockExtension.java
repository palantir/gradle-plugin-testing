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

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit extension that acquires and releases resource locks for Gradle daemon executors.
 * This ensures that each test has exclusive access to its assigned daemon.
 */
public final class DaemonResourceLockExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        // Only acquire lock if an executor has been assigned
        if (DaemonExecutorStore.hasExecutorIndex(context)) {
            int executorIndex = DaemonExecutorStore.getExecutorIndex(context);
            DaemonPoolManager.acquireLock(executorIndex);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        // Only release lock if an executor was assigned
        if (DaemonExecutorStore.hasExecutorIndex(context)) {
            int executorIndex = DaemonExecutorStore.getExecutorIndex(context);
            DaemonPoolManager.releaseLock(executorIndex);
        }
    }
}