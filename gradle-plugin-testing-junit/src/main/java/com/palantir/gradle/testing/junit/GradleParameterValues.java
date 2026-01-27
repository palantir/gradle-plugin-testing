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

import com.google.common.base.Splitter;
import com.palantir.gradle.testing.execution.GradleVersion;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * Computes parameter values for a test method based on the current Gradle version.
 *
 * <p>This class handles the logic for resolving which values should be used for each
 * {@link GradleParameter} annotation based on version conditions, and generates the
 * Cartesian product of all parameter values for test invocation generation.
 */
final class GradleParameterValues {

    /**
     * Computes all parameter value combinations for a test method for the given Gradle version.
     *
     * @param method the test method
     * @param gradleVersion the current Gradle version
     * @return list of parameter value maps, where each map represents one test invocation
     */
    public static List<Map<String, Object>> computeInvocations(Method method, GradleVersion gradleVersion) {
        List<GradleParameter> parameters = getGradleParameters(method);

        if (parameters.isEmpty()) {
            return List.of();
        }

        // Compute values for each parameter
        List<ParameterWithValues> parameterValues = parameters.stream()
                .map(param -> new ParameterWithValues(param.name(), computeValuesForParameter(param, gradleVersion)))
                .toList();

        // Generate Cartesian product
        return cartesianProduct(parameterValues);
    }

    /**
     * Checks if a method has any GradleParameter annotations.
     */
    public static boolean hasGradleParameters(Method method) {
        return !getGradleParameters(method).isEmpty();
    }

    private static List<GradleParameter> getGradleParameters(Method method) {
        // getAnnotationsByType properly handles both single and repeated annotations
        return Arrays.asList(method.getAnnotationsByType(GradleParameter.class));
    }

    private static List<Object> computeValuesForParameter(GradleParameter param, GradleVersion gradleVersion) {
        validateParameterTypes(param);

        List<Object> values = new ArrayList<>();
        ParameterType type = determineParameterType(param);

        // Collect values from all matching ForVersion conditions
        for (ForVersion forVersion : param.value()) {
            if (matchesVersion(forVersion, gradleVersion)) {
                values.addAll(extractValues(forVersion, type));
            }
        }

        // If no version conditions matched, use the otherwise values
        if (values.isEmpty()) {
            values.addAll(extractOtherwiseValues(param, type));
        }

        if (values.isEmpty()) {
            throw new IllegalStateException(String.format(
                    "No matching values found for parameter '%s' with Gradle version %s. "
                            + "Ensure either a version condition matches or an otherwise value is specified.",
                    param.name(), gradleVersion));
        }

        return values;
    }

    private static boolean matchesVersion(ForVersion forVersion, GradleVersion currentVersion) {
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
            return currentVersion.version().equals(equalTo);
        }

        return compareVersions(currentVersion.version(), lessThan) < 0;
    }

    private static int compareVersions(String version1, String version2) {
        List<String> parts1 = Splitter.on('.').splitToList(version1);
        List<String> parts2 = Splitter.on('.').splitToList(version2);

        int maxLength = Math.max(parts1.size(), parts2.size());

        for (int i = 0; i < maxLength; i++) {
            int v1 = i < parts1.size() ? parseVersionPart(parts1.get(i)) : 0;
            int v2 = i < parts2.size() ? parseVersionPart(parts2.get(i)) : 0;

            if (v1 != v2) {
                return Integer.compare(v1, v2);
            }
        }

        return 0;
    }

    private static int parseVersionPart(String part) {
        // Handle pre-release versions like "1-rc-1" by taking only the numeric prefix
        StringBuilder numeric = new StringBuilder();
        for (int i = 0; i < part.length(); i++) {
            char ch = part.charAt(i);
            if (Character.isDigit(ch)) {
                numeric.append(ch);
            } else {
                break;
            }
        }
        return numeric.isEmpty() ? 0 : Integer.parseInt(numeric.toString());
    }

    private static void validateParameterTypes(GradleParameter param) {
        ParameterType type = determineParameterType(param);

        for (ForVersion forVersion : param.value()) {
            ParameterType forVersionType = getForVersionType(forVersion);
            if (forVersionType != null && forVersionType != type) {
                throw new IllegalStateException(
                        "Parameter '%s' uses %s for otherwise but ForVersion specifies %s"
                                .formatted(
                                        param.name(),
                                        type.name().toLowerCase(Locale.ROOT),
                                        forVersionType.name().toLowerCase(Locale.ROOT)));
            }
        }
    }

    private static ParameterType getForVersionType(ForVersion forVersion) {
        int typesSpecified = 0;
        ParameterType type = null;

        if (forVersion.strings().length > 0) {
            typesSpecified++;
            type = ParameterType.STRING;
        }
        if (forVersion.ints().length > 0) {
            typesSpecified++;
            type = ParameterType.INT;
        }
        if (forVersion.longs().length > 0) {
            typesSpecified++;
            type = ParameterType.LONG;
        }
        if (forVersion.doubles().length > 0) {
            typesSpecified++;
            type = ParameterType.DOUBLE;
        }
        if (forVersion.booleans().length > 0) {
            typesSpecified++;
            type = ParameterType.BOOLEAN;
        }

        if (typesSpecified > 1) {
            throw new IllegalStateException("ForVersion cannot specify multiple value types");
        }

        return type;
    }

    private static ParameterType determineParameterType(GradleParameter param) {
        int otherwiseTypesSpecified = 0;
        ParameterType otherwiseType = null;

        if (param.otherwiseStrings().length > 0) {
            otherwiseTypesSpecified++;
            otherwiseType = ParameterType.STRING;
        }
        if (param.otherwiseInt().length > 0) {
            otherwiseTypesSpecified++;
            otherwiseType = ParameterType.INT;
        }
        if (param.otherwiseLong().length > 0) {
            otherwiseTypesSpecified++;
            otherwiseType = ParameterType.LONG;
        }
        if (param.otherwiseDouble().length > 0) {
            otherwiseTypesSpecified++;
            otherwiseType = ParameterType.DOUBLE;
        }
        if (param.otherwiseBoolean().length > 0) {
            otherwiseTypesSpecified++;
            otherwiseType = ParameterType.BOOLEAN;
        }

        if (otherwiseTypesSpecified > 1) {
            throw new IllegalStateException(
                    "GradleParameter '%s' cannot specify multiple otherwise value types".formatted(param.name()));
        }

        if (otherwiseType != null) {
            return otherwiseType;
        }

        // Infer from ForVersion values
        for (ForVersion forVersion : param.value()) {
            ParameterType forVersionType = getForVersionType(forVersion);
            if (forVersionType != null) {
                return forVersionType;
            }
        }

        throw new IllegalStateException(
                "Cannot determine parameter type for '%s'. Specify an otherwise value."
                        .formatted(param.name()));
    }

    private static List<Object> extractValues(ForVersion forVersion, ParameterType type) {
        return switch (type) {
            case STRING -> Arrays.stream(forVersion.strings()).map(s -> (Object) s).toList();
            case INT -> IntStream.of(forVersion.ints()).boxed().map(i -> (Object) i).toList();
            case LONG -> LongStream.of(forVersion.longs()).boxed().map(l -> (Object) l).toList();
            case DOUBLE -> DoubleStream.of(forVersion.doubles()).boxed().map(d -> (Object) d).toList();
            case BOOLEAN -> {
                boolean[] booleans = forVersion.booleans();
                List<Object> result = new ArrayList<>(booleans.length);
                for (boolean b : booleans) {
                    result.add(b);
                }
                yield result;
            }
        };
    }

    private static List<Object> extractOtherwiseValues(GradleParameter param, ParameterType type) {
        return switch (type) {
            case STRING -> Arrays.stream(param.otherwiseStrings()).map(s -> (Object) s).toList();
            case INT -> IntStream.of(param.otherwiseInt()).boxed().map(i -> (Object) i).toList();
            case LONG -> LongStream.of(param.otherwiseLong()).boxed().map(l -> (Object) l).toList();
            case DOUBLE -> DoubleStream.of(param.otherwiseDouble()).boxed().map(d -> (Object) d).toList();
            case BOOLEAN -> {
                boolean[] booleans = param.otherwiseBoolean();
                List<Object> result = new ArrayList<>(booleans.length);
                for (boolean b : booleans) {
                    result.add(b);
                }
                yield result;
            }
        };
    }

    private static List<Map<String, Object>> cartesianProduct(List<ParameterWithValues> parameters) {
        if (parameters.isEmpty()) {
            return List.of(Map.of());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        result.add(new LinkedHashMap<>());

        for (ParameterWithValues param : parameters) {
            List<Map<String, Object>> newResult = new ArrayList<>();

            for (Map<String, Object> existing : result) {
                for (Object value : param.values()) {
                    Map<String, Object> newMap = new LinkedHashMap<>(existing);
                    newMap.put(param.name(), value);
                    newResult.add(newMap);
                }
            }

            result = newResult;
        }

        return result;
    }

    private enum ParameterType {
        STRING,
        INT,
        LONG,
        DOUBLE,
        BOOLEAN
    }

    private record ParameterWithValues(String name, List<Object> values) {}

    private GradleParameterValues() {}
}
