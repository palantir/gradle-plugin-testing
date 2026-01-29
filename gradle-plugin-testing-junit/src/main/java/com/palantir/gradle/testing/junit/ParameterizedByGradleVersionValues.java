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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Computes parameter values for {@link ParameterizedByGradleVersion}. */
final class ParameterizedByGradleVersionValues {

    public static Optional<String> computeValue(Method method, String parameterName, GradleVersion gradleVersion) {
        ParameterizedByGradleVersion[] annotations = method.getAnnotationsByType(ParameterizedByGradleVersion.class);

        validateNoDuplicateNames(annotations, method);

        return Arrays.stream(annotations)
                .filter(anno -> anno.name().equals(parameterName))
                .findFirst()
                .map(anno -> {
                    validateOrdering(anno.when(), method);
                    return computeValueForAnnotation(anno, gradleVersion);
                });
    }

    private static String computeValueForAnnotation(ParameterizedByGradleVersion annotation, GradleVersion version) {
        return Arrays.stream(annotation.when())
                .filter(when -> version.compareTo(new GradleVersion(when.lessThan())) < 0)
                .map(WhenVersion::stringValue)
                .findFirst()
                .orElseGet(annotation::otherwiseString);
    }

    private static void validateNoDuplicateNames(ParameterizedByGradleVersion[] annotations, Method method) {
        Set<String> names = Arrays.stream(annotations)
                .map(ParameterizedByGradleVersion::name)
                .collect(Collectors.toSet());

        if (names.size() != annotations.length) {
            throw new IllegalStateException("@ParameterizedByGradleVersion on %s.%s has duplicate name values"
                    .formatted(method.getDeclaringClass().getSimpleName(), method.getName()));
        }
    }

    private static void validateOrdering(WhenVersion[] conditions, Method method) {
        List<GradleVersion> versions = Arrays.stream(conditions)
                .map(when -> new GradleVersion(when.lessThan()))
                .toList();

        boolean outOfOrder = IntStream.range(1, versions.size())
                .anyMatch(i -> versions.get(i - 1).compareTo(versions.get(i)) >= 0);

        if (outOfOrder) {
            throw new IllegalStateException(
                    ("@ParameterizedByGradleVersion on %s.%s must have @WhenVersion conditions ordered by ascending "
                                    + "lessThan version (lowest first)")
                            .formatted(method.getDeclaringClass().getSimpleName(), method.getName()));
        }
    }

    private ParameterizedByGradleVersionValues() {}
}
