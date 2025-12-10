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

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import one.util.streamex.IntStreamEx;

/**
 * Manages a pool of Gradle daemon executors with resource locking.
 * Each executor in the pool has a dedicated semaphore to ensure exclusive access.
 */
public final class DaemonPoolManager {
    /**
     * Magic constant: number of Gradle daemons in the pool.
     * This determines the maximum level of parallelism for Gradle test execution.
     */
    public static final int DAEMON_POOL_SIZE = 1;

    private static final String RESOURCE_LOCK_PREFIX = "gradle-daemon-";
    private static final List<NamedToolingApiGradleExecutor> EXECUTOR_POOL;
    private static final List<Semaphore> EXECUTOR_LOCKS;
    private static final AtomicInteger CURRENT_EXECUTOR = new AtomicInteger(0);

    static {
        EXECUTOR_POOL = IntStreamEx.range(DAEMON_POOL_SIZE)
                .mapToObj(i -> new NamedToolingApiGradleExecutor("test-kit-daemon-" + i))
                .toList();

        // Create a semaphore for each executor (with 1 permit = exclusive access)
        EXECUTOR_LOCKS = IntStreamEx.range(DAEMON_POOL_SIZE)
                .mapToObj(_i -> new Semaphore(1))
                .toList();
    }

    private DaemonPoolManager() {}

    /**
     * Result of executor assignment, containing both the executor and its index.
     */
    public static final class ExecutorAssignment {
        private final NamedToolingApiGradleExecutor executor;
        private final int index;

        ExecutorAssignment(NamedToolingApiGradleExecutor executor, int index) {
            this.executor = executor;
            this.index = index;
        }

        public NamedToolingApiGradleExecutor executor() {
            return executor;
        }

        public int index() {
            return index;
        }
    }

    /**
     * Gets the next executor from the pool in round-robin fashion.
     * Each test method will get a different executor.
     * Returns both the executor and its index for resource locking.
     */
    public static ExecutorAssignment assignNextExecutor() {
        int executorIndex = CURRENT_EXECUTOR.getAndIncrement() % DAEMON_POOL_SIZE;
        NamedToolingApiGradleExecutor executor = EXECUTOR_POOL.get(executorIndex);
        System.err.println("Assigning executor: " + executor + " (index: " + executorIndex + ")");
        return new ExecutorAssignment(executor, executorIndex);
    }

    /**
     * Acquires the lock for the specified executor.
     * This must be called before using the executor, and releaseLock must be called after.
     */
    public static void acquireLock(int executorIndex) {
        try {
            System.err.println("Acquiring lock for executor " + executorIndex);
            EXECUTOR_LOCKS.get(executorIndex).acquire();
            System.err.println("Lock acquired for executor " + executorIndex);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for executor lock " + executorIndex, e);
        }
    }

    /**
     * Releases the lock for the specified executor.
     */
    public static void releaseLock(int executorIndex) {
        System.err.println("Releasing lock for executor " + executorIndex);
        EXECUTOR_LOCKS.get(executorIndex).release();
    }

    /**
     * Gets the resource lock name for a specific executor index.
     * This is used for logging and debugging purposes.
     */
    public static String getResourceLockName(int executorIndex) {
        return RESOURCE_LOCK_PREFIX + executorIndex;
    }
}
