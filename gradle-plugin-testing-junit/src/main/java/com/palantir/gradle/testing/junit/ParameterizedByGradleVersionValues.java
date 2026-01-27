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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Computes parameter values for a test method based on the current Gradle version.
 */
final class ParameterizedByGradleVersionValues {

    public static List<Map<String, Object>> computeInvocations(Method method, GradleVersion gradleVersion) {
        List<ParameterizedByGradleVersion> parameters =
                Arrays.asList(method.getAnnotationsByType(ParameterizedByGradleVersion.class));

        if (parameters.isEmpty()) {
            return List.of();
        }

        List<String> names = parameters.stream().map(ParameterizedByGradleVersion::name).toList();
        List<List<Object>> valueLists = parameters.stream()
                .map(param -> computeValuesForParameter(param, gradleVersion))
                .toList();

        return Lists.cartesianProduct(valueLists).stream()
                .map(values -> toMap(names, values))
                .toList();
    }

    public static boolean hasGradleParameters(Method method) {
        return method.getAnnotationsByType(ParameterizedByGradleVersion.class).length > 0;
    }

    private static Map<String, Object> toMap(List<String> names, List<Object> values) {
        Map<String, Object> map = new LinkedHashMap<>();
        IntStream.range(0, names.size()).forEach(i -> map.put(names.get(i), values.get(i)));
        return map;
    }

    private static List<Object> computeValuesForParameter(
            ParameterizedByGradleVersion param, GradleVersion gradleVersion) {
        ValueType type = determineAndValidateType(param);

        List<Object> values = Arrays.stream(param.value())
                .filter(wv -> matchesVersion(wv, gradleVersion))
                .flatMap(wv -> type.fromWhenVersion(wv).stream())
                .toList();

        if (values.isEmpty()) {
            values = type.fromOtherwise(param);
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

    private static boolean matchesVersion(WhenVersion wv, GradleVersion current) {
        String equalTo = wv.equalTo();
        String lessThan = wv.lessThan();
        String lessThanOrEqualTo = wv.lessThanOrEqualTo();

        long specifiedCount = java.util.stream.Stream.of(equalTo, lessThan, lessThanOrEqualTo)
                .filter(s -> !s.isEmpty())
                .count();

        if (specifiedCount == 0) {
            throw new IllegalStateException(
                    "WhenVersion must specify exactly one of equalTo, lessThan, or lessThanOrEqualTo");
        }

        if (specifiedCount > 1) {
            throw new IllegalStateException(
                    "WhenVersion cannot specify multiple version conditions. Found equalTo='%s', lessThan='%s', lessThanOrEqualTo='%s'"
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

    private static ValueType determineAndValidateType(ParameterizedByGradleVersion param) {
        Optional<ValueType> otherwiseType = ValueType.detectFromOtherwise(param);

        for (WhenVersion wv : param.value()) {
            Optional<ValueType> wvType = ValueType.detectFromWhenVersion(wv);
            if (wvType.isPresent() && otherwiseType.isPresent() && wvType.get() != otherwiseType.get()) {
                throw new IllegalStateException("Parameter '%s' uses %s for otherwise but WhenVersion specifies %s"
                        .formatted(param.name(), otherwiseType.get().name, wvType.get().name));
            }
        }

        if (otherwiseType.isPresent()) {
            return otherwiseType.get();
        }

        for (WhenVersion wv : param.value()) {
            Optional<ValueType> wvType = ValueType.detectFromWhenVersion(wv);
            if (wvType.isPresent()) {
                return wvType.get();
            }
        }

        throw new IllegalStateException(
                "Cannot determine parameter type for '%s'. Specify an otherwise value.".formatted(param.name()));
    }

    private enum ValueType {
        STRING("string"),
        INT("int"),
        LONG("long"),
        DOUBLE("double"),
        BOOLEAN("boolean");

        private final String name;

        ValueType(String name) {
            this.name = name;
        }

        List<Object> fromWhenVersion(WhenVersion wv) {
            return switch (this) {
                case STRING -> Arrays.asList((Object[]) wv.strings());
                case INT -> IntStream.of(wv.ints()).boxed().map(i -> (Object) i).toList();
                case LONG -> Arrays.stream(wv.longs()).boxed().map(l -> (Object) l).toList();
                case DOUBLE -> Arrays.stream(wv.doubles()).boxed().map(d -> (Object) d).toList();
                case BOOLEAN -> boxBooleans(wv.booleans());
            };
        }

        List<Object> fromOtherwise(ParameterizedByGradleVersion p) {
            return switch (this) {
                case STRING -> Arrays.asList((Object[]) p.otherwiseStrings());
                case INT -> IntStream.of(p.otherwiseInt()).boxed().map(i -> (Object) i).toList();
                case LONG -> Arrays.stream(p.otherwiseLong()).boxed().map(l -> (Object) l).toList();
                case DOUBLE -> Arrays.stream(p.otherwiseDouble()).boxed().map(d -> (Object) d).toList();
                case BOOLEAN -> boxBooleans(p.otherwiseBoolean());
            };
        }

        boolean hasWhenVersionValue(WhenVersion wv) {
            return switch (this) {
                case STRING -> wv.strings().length > 0;
                case INT -> wv.ints().length > 0;
                case LONG -> wv.longs().length > 0;
                case DOUBLE -> wv.doubles().length > 0;
                case BOOLEAN -> wv.booleans().length > 0;
            };
        }

        boolean hasOtherwiseValue(ParameterizedByGradleVersion p) {
            return switch (this) {
                case STRING -> p.otherwiseStrings().length > 0;
                case INT -> p.otherwiseInt().length > 0;
                case LONG -> p.otherwiseLong().length > 0;
                case DOUBLE -> p.otherwiseDouble().length > 0;
                case BOOLEAN -> p.otherwiseBoolean().length > 0;
            };
        }

        static Optional<ValueType> detectFromWhenVersion(WhenVersion wv) {
            List<ValueType> types =
                    Arrays.stream(values()).filter(t -> t.hasWhenVersionValue(wv)).toList();
            if (types.size() > 1) {
                throw new IllegalStateException("WhenVersion cannot specify multiple value types");
            }
            return types.isEmpty() ? Optional.empty() : Optional.of(types.get(0));
        }

        static Optional<ValueType> detectFromOtherwise(ParameterizedByGradleVersion param) {
            List<ValueType> types =
                    Arrays.stream(values()).filter(t -> t.hasOtherwiseValue(param)).toList();
            if (types.size() > 1) {
                throw new IllegalStateException(
                        "ParameterizedByGradleVersion '%s' cannot specify multiple otherwise value types"
                                .formatted(param.name()));
            }
            return types.isEmpty() ? Optional.empty() : Optional.of(types.get(0));
        }

        private static List<Object> boxBooleans(boolean[] arr) {
            Object[] result = new Object[arr.length];
            for (int i = 0; i < arr.length; i++) {
                result[i] = arr[i];
            }
            return Arrays.asList(result);
        }
    }

    private ParameterizedByGradleVersionValues() {}
}
