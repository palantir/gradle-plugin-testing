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

import com.palantir.gradle.testing.project.RootProject;
import com.palantir.gradle.testing.project.SubProject;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.platform.commons.annotation.Testable;

final class GradleProjectParameterResolver implements TerseParameterResolver {
    @Override
    public Optional<Object> parameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        if (parameterContext.getParameter().getType().equals(RootProject.class)) {
            return Optional.of(rootProjectFor(extensionContext));
        }

        if (parameterContext.getParameter().getType().equals(SubProject.class)) {
            if (!isTestMethod(parameterContext)) {
                throw new IllegalStateException("""
                    SubProject parameters can only be injected in test methods \
                    (@Test, @ParameterizedTest, @RepeatedTest, @TestFactory, etc.), \
                    not in lifecycle methods like @BeforeEach or @AfterEach. \
                    Use RootProject.subproject("name") explicitly in lifecycle methods instead. \
                    Found SubProject parameter '%s' in method '%s'\
                    """.formatted(
                                parameterContext.getParameter().getName(),
                                parameterContext.getDeclaringExecutable().getName()));
            }
            return Optional.of(rootProjectFor(extensionContext)
                    .subproject(parameterContext.getParameter().getName()));
        }

        return Optional.empty();
    }

    private RootProject rootProjectFor(ExtensionContext extensionContext) {
        return RootProjectStore.rootProject(extensionContext);
    }

    private boolean isTestMethod(ParameterContext parameterContext) {
        return Arrays.stream(parameterContext.getDeclaringExecutable().getAnnotations())
                .anyMatch(annotation -> annotation.annotationType().isAnnotationPresent(Testable.class)
                        || annotation.annotationType().isAnnotationPresent(TestTemplate.class));
    }
}
