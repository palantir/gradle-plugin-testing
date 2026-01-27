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

import com.palantir.gradle.testing.execution.GradleVersion;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

/**
 * Provides test template invocation contexts for methods annotated with {@link GradleParameter}.
 *
 * <p>This extension generates multiple test invocations for a single test method, one for each
 * combination of parameter values that apply to the current Gradle version. Only invocations
 * that will actually run are created - no skipped tests are generated.
 */
final class GradleParameterTestTemplateProvider implements TestTemplateInvocationContextProvider {

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        // Only support if we have a GradleVersion in the store (set by @GradlePluginTests)
        // This prevents fixture classes without the proper test context from being discovered
        if (GradleVersionStore.gradleVersion(context) == null) {
            return false;
        }
        return context.getTestMethod()
                .map(GradleParameterValues::hasGradleParameters)
                .orElse(false);
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        Method method = context.getRequiredTestMethod();
        GradleVersion gradleVersion = GradleVersionStore.gradleVersion(context);

        List<Map<String, Object>> invocations = GradleParameterValues.computeInvocations(method, gradleVersion);

        if (invocations.isEmpty()) {
            // No parameter values for this Gradle version - this shouldn't happen
            // if the annotation is properly configured, but return empty to be safe
            return Stream.empty();
        }

        return invocations.stream().map(GradleParameterInvocationContext::new);
    }

    private record GradleParameterInvocationContext(Map<String, Object> parameterValues)
            implements TestTemplateInvocationContext {

        @Override
        public String getDisplayName(int invocationIndex) {
            if (parameterValues.size() == 1) {
                // Single parameter: just show the value
                return parameterValues.values().iterator().next().toString();
            }
            // Multiple parameters: show name=value pairs
            return parameterValues.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(", "));
        }

        @Override
        public List<Extension> getAdditionalExtensions() {
            return List.of(new GradleParameterResolver(parameterValues));
        }
    }
}
