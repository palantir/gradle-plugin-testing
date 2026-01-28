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

import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

final class ParameterizedByGradleVersionResolver implements TerseParameterResolver {

    private final List<String> parameterValues;

    ParameterizedByGradleVersionResolver(List<String> parameterValues) {
        this.parameterValues = parameterValues;
    }

    @Override
    public Optional<Object> parameter(ParameterContext parameterContext, ExtensionContext _extensionContext)
            throws ParameterResolutionException {
        if (parameterContext.getParameter().getType() != String.class) {
            return Optional.empty();
        }

        int stringIndex = countPrecedingStringParams(parameterContext);

        return stringIndex < parameterValues.size() ? Optional.of(parameterValues.get(stringIndex)) : Optional.empty();
    }

    private static int countPrecedingStringParams(ParameterContext ctx) {
        Parameter[] params = ctx.getDeclaringExecutable().getParameters();
        return (int) Arrays.stream(params, 0, ctx.getIndex())
                .filter(p -> p.getType() == String.class)
                .count();
    }
}
