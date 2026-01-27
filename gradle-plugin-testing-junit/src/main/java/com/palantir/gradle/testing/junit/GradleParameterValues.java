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
import com.google.common.primitives.Booleans;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.palantir.gradle.testing.execution.GradleVersion;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Computes parameter values for a test method based on the current Gradle version.
 */
final class GradleParameterValues {

    public static List<Map<String, Object>> computeInvocations(Method method, GradleVersion gradleVersion) {
        List<GradleParameter> parameters = Arrays.asList(method.getAnnotationsByType(GradleParameter.class));

        if (parameters.isEmpty()) {
            return List.of();
        }

        List<String> names = parameters.stream().map(GradleParameter::name).toList();
        List<List<Object>> valueLists = parameters.stream()
                .map(param -> computeValuesForParameter(param, gradleVersion))
                .toList();

        return Lists.cartesianProduct(valueLists).stream()
                .map(values -> toMap(names, values))
                .toList();
    }

    public static boolean hasGradleParameters(Method method) {
        return method.getAnnotationsByType(GradleParameter.class).length > 0;
    }

    private static Map<String, Object> toMap(List<String> names, List<Object> values) {
        Map<String, Object> map = new LinkedHashMap<>();
        IntStream.range(0, names.size()).forEach(i -> map.put(names.get(i), values.get(i)));
        return map;
    }

    private static List<Object> computeValuesForParameter(GradleParameter param, GradleVersion gradleVersion) {
        ParameterType type = determineAndValidateType(param);

        List<Object> values = Arrays.stream(param.value())
                .filter(fv -> matchesVersion(fv, gradleVersion))
                .flatMap(fv -> type.extractFromForVersion(fv).stream())
                .toList();

        if (values.isEmpty()) {
            values = type.extractFromOtherwise(param);
        }

        if (values.isEmpty()) {
            throw new IllegalStateException(String.format(
                    "No matching values found for parameter '%s' with Gradle version %s. "
                            + "Ensure either a version condition matches or an otherwise value is specified.",
                    param.name(),
                    gradleVersion));
        }

        return values;
    }

    private static boolean matchesVersion(ForVersion forVersion, GradleVersion current) {
        String equalTo = forVersion.equalTo();
        String lessThan = forVersion.lessThan();

        if (!equalTo.isEmpty() && !lessThan.isEmpty()) {
            throw new IllegalStateException(
                    "ForVersion cannot specify both equalTo and lessThan. Found equalTo='%s' and lessThan='%s'"
                            .formatted(equalTo, lessThan));
        }

        if (equalTo.isEmpty() && lessThan.isEmpty()) {
            throw new IllegalStateException("ForVersion must specify either equalTo or lessThan");
        }

        if (!equalTo.isEmpty()) {
            return current.isEqualTo(equalTo);
        }

        return current.isLessThan(lessThan);
    }

    private static ParameterType determineAndValidateType(GradleParameter param) {
        Optional<ParameterType> otherwiseType = ParameterType.fromOtherwise(param);

        for (ForVersion forVersion : param.value()) {
            Optional<ParameterType> forVersionType = ParameterType.fromForVersion(forVersion);
            if (forVersionType.isPresent()
                    && otherwiseType.isPresent()
                    && forVersionType.get() != otherwiseType.get()) {
                throw new IllegalStateException("Parameter '%s' uses %s for otherwise but ForVersion specifies %s"
                        .formatted(param.name(), otherwiseType.get().displayName(), forVersionType.get().displayName()));
            }
        }

        if (otherwiseType.isPresent()) {
            return otherwiseType.get();
        }

        for (ForVersion forVersion : param.value()) {
            Optional<ParameterType> forVersionType = ParameterType.fromForVersion(forVersion);
            if (forVersionType.isPresent()) {
                return forVersionType.get();
            }
        }

        throw new IllegalStateException(
                "Cannot determine parameter type for '%s'. Specify an otherwise value.".formatted(param.name()));
    }

    private enum ParameterType {
        STRING("string"),
        INT("int"),
        LONG("long"),
        DOUBLE("double"),
        BOOLEAN("boolean");

        private final String displayName;

        ParameterType(String displayName) {
            this.displayName = displayName;
        }

        String displayName() {
            return displayName;
        }

        boolean hasForVersionValue(ForVersion fv) {
            return switch (this) {
                case STRING -> fv.strings().length > 0;
                case INT -> fv.ints().length > 0;
                case LONG -> fv.longs().length > 0;
                case DOUBLE -> fv.doubles().length > 0;
                case BOOLEAN -> fv.booleans().length > 0;
            };
        }

        List<Object> extractFromForVersion(ForVersion fv) {
            return switch (this) {
                case STRING -> Arrays.asList((Object[]) fv.strings());
                case INT -> Ints.asList(fv.ints()).stream().map(i -> (Object) i).toList();
                case LONG -> Longs.asList(fv.longs()).stream().map(l -> (Object) l).toList();
                case DOUBLE -> Doubles.asList(fv.doubles()).stream().map(d -> (Object) d).toList();
                case BOOLEAN -> Booleans.asList(fv.booleans()).stream().map(b -> (Object) b).toList();
            };
        }

        boolean hasOtherwiseValue(GradleParameter p) {
            return switch (this) {
                case STRING -> p.otherwiseStrings().length > 0;
                case INT -> p.otherwiseInt().length > 0;
                case LONG -> p.otherwiseLong().length > 0;
                case DOUBLE -> p.otherwiseDouble().length > 0;
                case BOOLEAN -> p.otherwiseBoolean().length > 0;
            };
        }

        List<Object> extractFromOtherwise(GradleParameter p) {
            return switch (this) {
                case STRING -> Arrays.asList((Object[]) p.otherwiseStrings());
                case INT -> Ints.asList(p.otherwiseInt()).stream().map(i -> (Object) i).toList();
                case LONG -> Longs.asList(p.otherwiseLong()).stream().map(l -> (Object) l).toList();
                case DOUBLE -> Doubles.asList(p.otherwiseDouble()).stream().map(d -> (Object) d).toList();
                case BOOLEAN -> Booleans.asList(p.otherwiseBoolean()).stream().map(b -> (Object) b).toList();
            };
        }

        static Optional<ParameterType> fromForVersion(ForVersion forVersion) {
            List<ParameterType> types = Arrays.stream(values())
                    .filter(type -> type.hasForVersionValue(forVersion))
                    .toList();

            if (types.size() > 1) {
                throw new IllegalStateException("ForVersion cannot specify multiple value types");
            }

            return types.isEmpty() ? Optional.empty() : Optional.of(types.get(0));
        }

        static Optional<ParameterType> fromOtherwise(GradleParameter param) {
            List<ParameterType> types = Arrays.stream(values())
                    .filter(type -> type.hasOtherwiseValue(param))
                    .toList();

            if (types.size() > 1) {
                throw new IllegalStateException(
                        "GradleParameter '%s' cannot specify multiple otherwise value types".formatted(param.name()));
            }

            return types.isEmpty() ? Optional.empty() : Optional.of(types.get(0));
        }
    }

    private GradleParameterValues() {}
}
