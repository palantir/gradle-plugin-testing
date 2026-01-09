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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * Utility class for Gradle versions based on annotations.
 */
final class GradleVersions {

    private static final String GRADLE_VERSIONS_CONFIG_PARAM = "com.palantir.gradle.testing.gradle_versions_to_test";

    /**
     * Returns all Gradle versions for the test class, including versions from all methods,
     * with the class-level {@link RestrictToGradleVersionsEqualTo} filter applied.
     *
     * <p>This is used to build the complete test matrix for the class.
     */
    static Set<GradleVersion> allFilteredVersions(ExtensionContext context) {
        Set<GradleVersion> versions = configuredVersions(context);

        context.getTestClass().ifPresent(clazz -> {
            versions.addAll(versionsFromAnnotation(clazz));

            Arrays.stream(clazz.getDeclaredMethods())
                    .filter(GradleVersions::isTestMethod)
                    .forEach(method -> versions.addAll(versionsFromAnnotation(method)));

            applyFilter(versions, clazz);
        });

        return versions;
    }

    /**
     * Returns all Gradle versions for a specific method, including class-level versions,
     * with the method-level {@link RestrictToGradleVersionsEqualTo} filter applied.
     *
     * <p>This is used to determine if a method should run for a given Gradle version.
     */
    static Set<GradleVersion> filteredVersionsForMethod(ExtensionContext context) {
        Set<GradleVersion> versions = configuredVersions(context);

        context.getTestClass().ifPresent(clazz -> versions.addAll(versionsFromAnnotation(clazz)));

        context.getTestMethod().ifPresent(method -> {
            versions.addAll(versionsFromAnnotation(method));
            applyFilter(versions, method);
        });

        return versions;
    }

    private static Set<GradleVersion> configuredVersions(ExtensionContext context) {
        return new LinkedHashSet<>(context.getConfigurationParameter(GRADLE_VERSIONS_CONFIG_PARAM)
                .map(param -> Splitter.on(',').splitToList(param).stream()
                        .map(GradleVersion::new)
                        .toList())
                .orElseThrow(() -> new RuntimeException("Not configured with the gradle versions to test against. "
                        + "Have you applied the `com.palantir.gradle-plugin-testing` plugin to this project?")));
    }

    private static Set<GradleVersion> versionsFromAnnotation(AnnotatedElement element) {
        return AnnotationSupport.findAnnotation(element, WithSpecialCaseGradleVersions.class).stream()
                .flatMap(annotation -> Arrays.stream(annotation.value()))
                .map(GradleVersion::new)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static void applyFilter(Set<GradleVersion> versions, AnnotatedElement element) {
        AnnotationSupport.findAnnotation(element, RestrictToGradleVersionsEqualTo.class)
                .ifPresent(annotation -> {
                    Set<GradleVersion> allowed = Arrays.stream(annotation.value())
                            .map(GradleVersion::new)
                            .collect(Collectors.toSet());
                    versions.retainAll(allowed);
                });
    }

    private static boolean isTestMethod(Method method) {
        return AnnotationSupport.isAnnotated(method, Test.class);
    }

    private GradleVersions() {}
}
