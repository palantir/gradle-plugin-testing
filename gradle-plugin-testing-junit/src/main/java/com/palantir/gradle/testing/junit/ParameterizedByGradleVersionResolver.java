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

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

/**
 * Resolves parameters for test methods annotated with {@link ParameterizedByGradleVersion}.
 *
 * <p>This resolver is instantiated for each test invocation with the specific parameter values
 * for that invocation.
 */
final class ParameterizedByGradleVersionResolver implements TerseParameterResolver {

    private final Map<String, Object> parameterValues;

    ParameterizedByGradleVersionResolver(Map<String, Object> parameterValues) {
        this.parameterValues = parameterValues;
    }

    @Override
    public Optional<Object> parameter(ParameterContext parameterContext, ExtensionContext _extensionContext)
            throws ParameterResolutionException {
        String parameterName = parameterContext.getParameter().getName();
        return Optional.ofNullable(parameterValues.get(parameterName));
    }
}
