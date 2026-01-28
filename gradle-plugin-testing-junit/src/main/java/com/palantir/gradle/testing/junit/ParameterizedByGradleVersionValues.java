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

import com.google.common.collect.Lists;
import com.palantir.gradle.testing.execution.GradleVersion;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/** Computes parameter values for {@link ParameterizedByGradleVersion} based on Gradle version. */
final class ParameterizedByGradleVersionValues {

    /** Returns all value combinations as a Cartesian product of each annotation's values. */
    public static List<List<String>> computeInvocations(Method method, GradleVersion gradleVersion) {
        List<ParameterizedByGradleVersion> parameters =
                Arrays.asList(method.getAnnotationsByType(ParameterizedByGradleVersion.class));

        if (parameters.isEmpty()) {
            return List.of();
        }

        List<List<String>> valueLists = parameters.stream()
                .map(param -> computeValuesForParameter(param, gradleVersion))
                .toList();

        return Lists.cartesianProduct(valueLists);
    }

    public static boolean hasGradleParameters(Method method) {
        return method.getAnnotationsByType(ParameterizedByGradleVersion.class).length > 0;
    }

    private static List<String> computeValuesForParameter(
            ParameterizedByGradleVersion param, GradleVersion gradleVersion) {
        List<String> values = Arrays.stream(param.value())
                .filter(wv -> matchesVersion(wv, gradleVersion))
                .flatMap(wv -> Arrays.stream(wv.value()))
                .toList();

        if (values.isEmpty()) {
            values = Arrays.asList(param.otherwise());
        }

        if (values.isEmpty()) {
            throw new IllegalStateException(
                    ("No matching values found for @ParameterizedByGradleVersion with Gradle version %s. "
                                    + "Ensure either a version condition matches or an otherwise value is specified.")
                            .formatted(gradleVersion));
        }

        return values;
    }

    private static boolean matchesVersion(WhenVersion wv, GradleVersion current) {
        String equalTo = wv.equalTo();
        String lessThan = wv.lessThan();
        String lessThanOrEqualTo = wv.lessThanOrEqualTo();

        long specifiedCount = Stream.of(equalTo, lessThan, lessThanOrEqualTo)
                .filter(s -> !s.isEmpty())
                .count();

        if (specifiedCount == 0) {
            throw new IllegalStateException(
                    "WhenVersion must specify exactly one of equalTo, lessThan, or lessThanOrEqualTo");
        }

        if (specifiedCount > 1) {
            throw new IllegalStateException(("WhenVersion cannot specify multiple version conditions. "
                            + "Found equalTo='%s', lessThan='%s', lessThanOrEqualTo='%s'")
                    .formatted(equalTo, lessThan, lessThanOrEqualTo));
        }

        if (!equalTo.isEmpty()) {
            return current.isEqualTo(equalTo);
        } else if (!lessThan.isEmpty()) {
            return current.isLessThan(lessThan);
        } else {
            return current.isLessThanOrEqualTo(lessThanOrEqualTo);
        }
    }

    private ParameterizedByGradleVersionValues() {}
}
