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

package com.palantir.gradle.testing.junit;

import com.palantir.gradle.testing.execution.DaemonExecutorStore;
import com.palantir.gradle.testing.execution.DaemonPoolManager;
import com.palantir.gradle.testing.execution.GradleInvoker;
import java.util.Optional;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

final class GradleInvokerParameterResolver implements TerseParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(GradleInvokerParameterResolver.class);
    private static final String GRADLE_INVOKER_KEY = "gradleInvoker";

    @Override
    public Optional<Object> parameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (parameterContext.getParameter().getType().equals(GradleInvoker.class)) {
            // Cache the GradleInvoker in the extension context to avoid creating it multiple times
            // (supportsParameter and resolveParameter both call this method)
            return Optional.of(extensionContext.getStore(NAMESPACE).getOrComputeIfAbsent(GRADLE_INVOKER_KEY, _key -> {
                // Assign an executor from the pool for this test method
                DaemonPoolManager.ExecutorAssignment assignment = DaemonPoolManager.assignNextExecutor();

                // Store the executor index in the context for resource locking
                DaemonExecutorStore.setExecutorIndex(extensionContext, assignment.index());

                return GradleInvoker.create(
                        RootProjectStore.rootProjectDir(extensionContext),
                        GradleVersionStore.gradleVersion(extensionContext),
                        ConfigurationCacheStore.isConfigurationCacheEnabled(extensionContext),
                        assignment.executor());
            }));
        }

        return Optional.empty();
    }
}
