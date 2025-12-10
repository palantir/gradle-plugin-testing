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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

/**
 * Stores the assigned executor index in JUnit's ExtensionContext for resource locking.
 */
public final class DaemonExecutorStore {
    private static final Namespace NAMESPACE = Namespace.create(DaemonExecutorStore.class);
    private static final String EXECUTOR_INDEX_KEY = "executorIndex";

    private DaemonExecutorStore() {}

    public static void setExecutorIndex(ExtensionContext context, int executorIndex) {
        context.getStore(NAMESPACE).put(EXECUTOR_INDEX_KEY, executorIndex);
    }

    public static int getExecutorIndex(ExtensionContext context) {
        Integer index = context.getStore(NAMESPACE).get(EXECUTOR_INDEX_KEY, Integer.class);
        if (index == null) {
            throw new IllegalStateException(
                    "Executor index not found in context. Make sure GradleInvokerParameterResolver has been called.");
        }
        return index;
    }

    public static boolean hasExecutorIndex(ExtensionContext context) {
        return context.getStore(NAMESPACE).get(EXECUTOR_INDEX_KEY, Integer.class) != null;
    }
}