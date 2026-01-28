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

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

/**
 * Provides test template invocation contexts for methods annotated with {@link ParameterizedByGradleVersion}.
 *
 * <p>This extension generates multiple test invocations for a single test method, one for each
 * combination of parameter values that apply to the current Gradle version.
 */
final class ParameterizedByGradleVersionTestTemplateProvider implements TestTemplateInvocationContextProvider {

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        // Only support if we have a GradleVersion in the store (set by @GradlePluginTests)
        // This prevents fixture classes without the proper test context from being discovered
        if (GradleVersionStore.gradleVersion(context) == null) {
            return false;
        }
        return context.getTestMethod()
                .map(ParameterizedByGradleVersionValues::hasGradleParameters)
                .orElse(false);
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        return ParameterizedByGradleVersionValues.computeInvocations(
                        context.getRequiredTestMethod(), GradleVersionStore.gradleVersion(context))
                .stream()
                .map(ParameterizedByGradleVersionInvocationContext::new);
    }

    private record ParameterizedByGradleVersionInvocationContext(List<String> parameterValues)
            implements TestTemplateInvocationContext {

        public String getDisplayName(int invocationIndex) {
            return String.join(", ", parameterValues);
        }

        @Override
        public List<Extension> getAdditionalExtensions() {
            return List.of(new ParameterizedByGradleVersionResolver(parameterValues));
        }
    }
}
