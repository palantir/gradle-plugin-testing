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
import java.util.Arrays;
import java.util.Optional;

/** Computes parameter values for {@link ParameterizedByGradleVersion}. */
final class ParameterizedByGradleVersionValues {

    public static Optional<String> computeValue(Method method, GradleVersion gradleVersion) {
        ParameterizedByGradleVersion annotation = method.getAnnotation(ParameterizedByGradleVersion.class);

        if (annotation == null) {
            return Optional.empty();
        }

        WhenVersion[] conditions = annotation.when();
        validateOrdering(conditions, method);

        return Arrays.stream(conditions)
                .filter(when -> gradleVersion.compareTo(new GradleVersion(when.lessThan())) < 0)
                .map(WhenVersion::stringValue)
                .findFirst()
                .or(() -> Optional.of(annotation.otherwiseString()));
    }

    public static Optional<String> parameterName(Method method) {
        return Optional.ofNullable(method.getAnnotation(ParameterizedByGradleVersion.class))
                .map(ParameterizedByGradleVersion::name);
    }

    private static void validateOrdering(WhenVersion[] conditions, Method method) {
        for (int i = 1; i < conditions.length; i++) {
            GradleVersion previous = new GradleVersion(conditions[i - 1].lessThan());
            GradleVersion current = new GradleVersion(conditions[i].lessThan());

            if (previous.compareTo(current) >= 0) {
                throw new IllegalStateException(
                        "@ParameterizedByGradleVersion on %s.%s must have @WhenVersion conditions ordered by ascending lessThan version (lowest first)"
                                .formatted(method.getDeclaringClass().getSimpleName(), method.getName()));
            }
        }
    }

    private ParameterizedByGradleVersionValues() {}
}
