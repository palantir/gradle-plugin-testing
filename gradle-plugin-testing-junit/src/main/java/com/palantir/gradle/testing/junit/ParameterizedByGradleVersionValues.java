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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Computes parameter value for {@link ParameterizedByGradleVersion} based on Gradle version. */
final class ParameterizedByGradleVersionValues {

    /**
     * Returns the single parameter value for the given Gradle version.
     *
     * <p>Validates that annotations cover the entire version space with no gaps or overlaps.
     */
    public static Optional<String> computeValue(Method method, GradleVersion gradleVersion) {
        List<ParameterizedByGradleVersion> annotations =
                Arrays.asList(method.getAnnotationsByType(ParameterizedByGradleVersion.class));

        if (annotations.isEmpty()) {
            return Optional.empty();
        }

        validateVersionRanges(annotations, method);

        return annotations.stream()
                .filter(anno -> matchesVersion(anno, gradleVersion))
                .map(ParameterizedByGradleVersion::stringValue)
                .findFirst();
    }

    private static boolean matchesVersion(ParameterizedByGradleVersion anno, GradleVersion current) {
        String lower = anno.lowerBound();
        String upper = anno.upperBound();

        boolean aboveLower = lower.isEmpty() || current.isGreaterThanOrEqualTo(lower);
        boolean belowUpper = upper.isEmpty() || current.isLessThan(upper);

        return aboveLower && belowUpper;
    }

    private static void validateVersionRanges(List<ParameterizedByGradleVersion> annotations, Method method) {
        // Sort by lower bound (empty string = 0.0.0, comes first)
        List<VersionRange> ranges = annotations.stream()
                .map(VersionRange::from)
                .sorted(Comparator.comparing(VersionRange::lowerBound, VersionRange::compareVersions))
                .toList();

        // Check first range starts at 0.0.0 (empty lower bound)
        if (!ranges.get(0).lowerBound().isEmpty()) {
            throw new IllegalStateException(
                    ("@ParameterizedByGradleVersion on %s.%s must have a range starting from 0.0.0 "
                                    + "(use empty lowerBound). First range starts at '%s'.")
                            .formatted(
                                    method.getDeclaringClass().getSimpleName(),
                                    method.getName(),
                                    ranges.get(0).lowerBound()));
        }

        // Check last range has no upper bound (extends to infinity)
        if (!ranges.get(ranges.size() - 1).upperBound().isEmpty()) {
            throw new IllegalStateException(
                    ("@ParameterizedByGradleVersion on %s.%s must have a range extending to infinity "
                                    + "(use empty upperBound). Last range ends at '%s'.")
                            .formatted(
                                    method.getDeclaringClass().getSimpleName(),
                                    method.getName(),
                                    ranges.get(ranges.size() - 1).upperBound()));
        }

        // Check ranges are contiguous (each upper bound equals next lower bound)
        for (int i = 0; i < ranges.size() - 1; i++) {
            VersionRange current = ranges.get(i);
            VersionRange next = ranges.get(i + 1);

            if (current.upperBound().isEmpty()) {
                throw new IllegalStateException(("@ParameterizedByGradleVersion on %s.%s has overlapping ranges. "
                                + "Range with stringValue='%s' has no upper bound but is not the last range.")
                        .formatted(
                                method.getDeclaringClass().getSimpleName(), method.getName(), current.stringValue()));
            }

            if (!current.upperBound().equals(next.lowerBound())) {
                throw new IllegalStateException(
                        ("@ParameterizedByGradleVersion on %s.%s has a gap or overlap between ranges. "
                                        + "Range '%s' ends at '%s' but next range '%s' starts at '%s'. "
                                        + "Upper bound of one range must equal lower bound of the next.")
                                .formatted(
                                        method.getDeclaringClass().getSimpleName(),
                                        method.getName(),
                                        current.stringValue(),
                                        current.upperBound(),
                                        next.stringValue(),
                                        next.lowerBound()));
            }
        }
    }

    private record VersionRange(String lowerBound, String upperBound, String stringValue) {
        static VersionRange from(ParameterizedByGradleVersion anno) {
            return new VersionRange(anno.lowerBound(), anno.upperBound(), anno.stringValue());
        }

        static int compareVersions(String v1, String v2) {
            if (v1.isEmpty() && v2.isEmpty()) {
                return 0;
            }
            if (v1.isEmpty()) {
                return -1; // empty, always less
            }
            if (v2.isEmpty()) {
                return 1;
            }
            return GradleVersion.of(v1).compareTo(GradleVersion.of(v2));
        }
    }

    private ParameterizedByGradleVersionValues() {}
}
