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
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ClassTemplateInvocationContext;
import org.junit.jupiter.api.extension.ClassTemplateInvocationContextProvider;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

final class GradleVersioningClassTemplate implements ClassTemplateInvocationContextProvider {
    @Override
    public boolean supportsClassTemplate(ExtensionContext context) {
        return true;
    }

    @Override
    public Stream<? extends ClassTemplateInvocationContext> provideClassTemplateInvocationContexts(
            ExtensionContext context) {
        return GradleVersionFilter.allFilteredVersions(context).stream().map(GradleVersionInvocationContext::new);
    }

    private record GradleVersionInvocationContext(GradleVersion gradleVersion)
            implements ClassTemplateInvocationContext {
        @Override
        public String getDisplayName(int invocationIndex) {
            return "Gradle " + gradleVersion;
        }

        @Override
        public void prepareInvocation(ExtensionContext context) {
            GradleVersionStore.setGradleVersion(context, gradleVersion);
        }

        @Override
        public List<Extension> getAdditionalExtensions() {
            return List.of(
                    new WithGradleVersionsCondition(),
                    new GradleInvokerParameterResolver(),
                    new GradleProjectParameterResolver(),
                    new MavenRepoParameterResolver());
        }
    }
}
