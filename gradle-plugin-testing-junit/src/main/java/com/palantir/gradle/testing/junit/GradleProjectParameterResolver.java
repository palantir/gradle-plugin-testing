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
import java.util.Optional;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;

final class GradleProjectParameterResolver implements TerseParameterResolver {
    @Override
    public Optional<Object> parameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        if (parameterContext.getParameter().getType().equals(RootProject.class)) {
            return Optional.of(rootProjectFor(extensionContext));
        }

        if (parameterContext.getParameter().getType().equals(SubProject.class)) {
            return Optional.of(rootProjectFor(extensionContext)
                    .subproject(parameterContext.getParameter().getName()));
        }

        return Optional.empty();
    }

    private RootProject rootProjectFor(ExtensionContext extensionContext) {
        return RootProjectStore.rootProject(extensionContext);
    }
}
