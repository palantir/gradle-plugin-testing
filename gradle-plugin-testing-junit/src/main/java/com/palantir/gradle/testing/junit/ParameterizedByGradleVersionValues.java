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

import com.google.common.collect.Comparators;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.palantir.gradle.testing.execution.GradleVersion;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Computes parameter values for {@link ParameterizedByGradleVersion}. */
final class ParameterizedByGradleVersionValues {

    public static Optional<String> computeValue(Method method, String parameterName, GradleVersion gradleVersion) {
        ParameterizedByGradleVersion[] annotations = method.getAnnotationsByType(ParameterizedByGradleVersion.class);

        if (annotations.length == 0) {
            return Optional.empty();
        }

        validate(annotations, method);

        return findMatchingAnnotation(annotations, parameterName).map(annotation -> {
            validateOrdering(annotation.when(), method);
            return computeValueForAnnotation(annotation, gradleVersion);
        });
    }

    private static Optional<ParameterizedByGradleVersion> findMatchingAnnotation(
            ParameterizedByGradleVersion[] annotations, String parameterName) {
        if (annotations.length == 1 && annotations[0].name().isEmpty()) {
            // Single annotation without name - matches any @InjectByGradleVersion parameter
            return Optional.of(annotations[0]);
        }

        // Multiple annotations or single with name - match by name
        return Arrays.stream(annotations)
                .filter(annotation -> annotation.name().equals(parameterName))
                .findFirst();
    }

    private static String computeValueForAnnotation(ParameterizedByGradleVersion annotation, GradleVersion version) {
        return Arrays.stream(annotation.when())
                .filter(when -> version.compareTo(new GradleVersion(when.lessThan())) < 0)
                .map(ParameterizedByGradleVersion.WhenVersion::stringValue)
                .findFirst()
                .orElseGet(annotation::otherwiseString);
    }

    private static void validate(ParameterizedByGradleVersion[] annotations, Method method) {
        if (annotations.length > 1) {
            validateAllHaveNames(annotations, method);
            validateNoDuplicateNames(annotations, method);
        }
        validateAnnotationsMatchParameters(annotations, method);
    }

    private static void validateAllHaveNames(ParameterizedByGradleVersion[] annotations, Method method) {
        boolean anyMissingName = Arrays.stream(annotations)
                .anyMatch(annotation -> annotation.name().isEmpty());

        if (anyMissingName) {
            throw new IllegalStateException(
                    "@ParameterizedByGradleVersion on %s.%s: name is required when multiple annotations are present"
                            .formatted(method.getDeclaringClass().getSimpleName(), method.getName()));
        }
    }

    private static void validateNoDuplicateNames(ParameterizedByGradleVersion[] annotations, Method method) {
        Multiset<String> nameCounts = HashMultiset.create();
        Arrays.stream(annotations).map(ParameterizedByGradleVersion::name).forEach(nameCounts::add);

        Set<String> duplicates = nameCounts.entrySet().stream()
                .filter(entry -> entry.getCount() > 1)
                .map(Multiset.Entry::getElement)
                .collect(Collectors.toSet());

        if (!duplicates.isEmpty()) {
            throw new IllegalStateException("@ParameterizedByGradleVersion on %s.%s has duplicate name values: %s"
                    .formatted(method.getDeclaringClass().getSimpleName(), method.getName(), duplicates));
        }
    }

    private static void validateAnnotationsMatchParameters(ParameterizedByGradleVersion[] annotations, Method method) {
        Map<String, ParameterizedByGradleVersion> annotationsByName = Arrays.stream(annotations)
                .collect(Collectors.toMap(ParameterizedByGradleVersion::name, Function.identity()));

        List<Parameter> injectParameters = Arrays.stream(method.getParameters())
                .filter(param -> param.isAnnotationPresent(InjectByGradleVersion.class))
                .toList();

        Set<String> injectParameterNames =
                injectParameters.stream().map(Parameter::getName).collect(Collectors.toSet());

        // For single unnamed annotation, it matches any single @InjectByGradleVersion parameter
        if (annotations.length == 1 && annotations[0].name().isEmpty()) {
            if (injectParameters.size() == 1) {
                throw new IllegalStateException(
                        ("@ParameterizedByGradleVersion on %s.%s without a name requires exactly one "
                                        + "@InjectByGradleVersion parameter (found %d)")
                                .formatted(
                                        method.getDeclaringClass().getSimpleName(),
                                        method.getName(),
                                        injectParameters.size()));
            }
            return;
        }

        // Validate: each annotation has a corresponding @InjectByGradleVersion parameter
        Set<String> annotationNames = annotationsByName.keySet();
        Set<String> missingParameters = annotationNames.stream()
                .filter(name -> !injectParameterNames.contains(name))
                .collect(Collectors.toSet());

        if (!missingParameters.isEmpty()) {
            throw new IllegalStateException(
                    "@ParameterizedByGradleVersion on %s.%s: no @InjectByGradleVersion parameter found for name(s): %s"
                            .formatted(
                                    method.getDeclaringClass().getSimpleName(), method.getName(), missingParameters));
        }

        // Validate: each @InjectByGradleVersion parameter has a corresponding annotation
        Set<String> missingAnnotations = injectParameterNames.stream()
                .filter(name -> !annotationNames.contains(name))
                .collect(Collectors.toSet());

        if (!missingAnnotations.isEmpty()) {
            throw new IllegalStateException(("@ParameterizedByGradleVersion on %s.%s: no annotation found for "
                            + "@InjectByGradleVersion parameter(s): %s")
                    .formatted(method.getDeclaringClass().getSimpleName(), method.getName(), missingAnnotations));
        }
    }

    private static void validateOrdering(ParameterizedByGradleVersion.WhenVersion[] conditions, Method method) {
        List<GradleVersion> versions = Arrays.stream(conditions)
                .map(when -> new GradleVersion(when.lessThan()))
                .toList();

        if (!Comparators.isInStrictOrder(versions, Comparator.naturalOrder())) {
            throw new IllegalStateException(
                    ("@ParameterizedByGradleVersion on %s.%s must have @WhenVersion conditions ordered by ascending "
                                    + "lessThan version (lowest first)")
                            .formatted(method.getDeclaringClass().getSimpleName(), method.getName()));
        }
    }

    private ParameterizedByGradleVersionValues() {}
}
