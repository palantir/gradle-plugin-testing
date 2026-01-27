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
 * Resolves parameters for test methods annotated with {@link GradleParameter}.
 *
 * <p>This resolver is instantiated for each test invocation with the specific parameter values
 * for that invocation.
 */
final class GradleParameterResolver implements TerseParameterResolver {

    private final Map<String, Object> parameterValues;

    GradleParameterResolver(Map<String, Object> parameterValues) {
        this.parameterValues = parameterValues;
    }

    @Override
    public Optional<Object> parameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        String parameterName = parameterContext.getParameter().getName();

        if (parameterValues.containsKey(parameterName)) {
            Object value = parameterValues.get(parameterName);
            Class<?> parameterType = parameterContext.getParameter().getType();

            // Handle type conversion
            if (parameterType == String.class && value instanceof String) {
                return Optional.of(value);
            }
            if ((parameterType == int.class || parameterType == Integer.class) && value instanceof Integer) {
                return Optional.of(value);
            }
            if ((parameterType == long.class || parameterType == Long.class) && value instanceof Long) {
                return Optional.of(value);
            }
            if ((parameterType == double.class || parameterType == Double.class) && value instanceof Double) {
                return Optional.of(value);
            }
            if ((parameterType == boolean.class || parameterType == Boolean.class) && value instanceof Boolean) {
                return Optional.of(value);
            }

            throw new ParameterResolutionException("Parameter '%s' has type %s but value is of type %s"
                    .formatted(
                            parameterName,
                            parameterType.getName(),
                            value.getClass().getName()));
        }

        return Optional.empty();
    }
}
