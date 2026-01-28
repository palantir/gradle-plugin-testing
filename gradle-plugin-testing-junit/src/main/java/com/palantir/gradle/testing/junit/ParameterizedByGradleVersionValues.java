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
import java.util.Set;
import java.util.stream.Collectors;

/** Computes parameter value for {@link ParameterizedByGradleVersion} based on Gradle version. */
final class ParameterizedByGradleVersionValues {

    /** Returns the parameter value for the given Gradle version, validating range coverage. */
    public static Optional<String> computeValue(Method method, GradleVersion gradleVersion) {
        ParameterizedByGradleVersion[] annotations = method.getAnnotationsByType(ParameterizedByGradleVersion.class);

        if (annotations.length == 0) {
            return Optional.empty();
        }

        validateVersionRanges(annotations, method);

        return Arrays.stream(annotations)
                .filter(anno -> matchesVersion(anno, gradleVersion))
                .map(ParameterizedByGradleVersion::stringValue)
                .findFirst();
    }

    private static boolean matchesVersion(ParameterizedByGradleVersion anno, GradleVersion current) {
        boolean aboveLower =
                anno.lowerBound().isEmpty() || current.compareTo(new GradleVersion(anno.lowerBound())) >= 0;
        boolean belowUpper = anno.upperBound().isEmpty() || current.compareTo(new GradleVersion(anno.upperBound())) < 0;
        return aboveLower && belowUpper;
    }

    private static void validateVersionRanges(ParameterizedByGradleVersion[] annotations, Method method) {
        // For contiguous ranges, the set of lower bounds must equal the set of upper bounds,
        // and there must be no duplicates within each set
        Set<String> lowerBounds = Arrays.stream(annotations)
                .map(ParameterizedByGradleVersion::lowerBound)
                .collect(Collectors.toSet());
        Set<String> upperBounds = Arrays.stream(annotations)
                .map(ParameterizedByGradleVersion::upperBound)
                .collect(Collectors.toSet());

        if (lowerBounds.size() != annotations.length || !lowerBounds.equals(upperBounds)) {
            throw new IllegalStateException(
                    "@ParameterizedByGradleVersion on %s.%s must have contiguous ranges covering all versions"
                            .formatted(method.getDeclaringClass().getSimpleName(), method.getName()));
        }
    }

    private ParameterizedByGradleVersionValues() {}
}
