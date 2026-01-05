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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * Execution condition that filters tests based on Gradle version.
 *
 * <p>When a method has its own {@link AdditionalGradleVersions} annotation, this condition ensures
 * that only the method-specific versions (plus base and class-level versions) run for that method.
 * For methods without the annotation, only base and class-level versions run.
 */
final class AdditionalGradleVersionsCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        // Only evaluate for test methods
        if (context.getTestMethod().isEmpty()) {
            return ConditionEvaluationResult.enabled("No test method");
        }

        GradleVersion currentVersion = GradleVersionStore.gradleVersion(context);

        Set<String> versionsForThisMethod = getVersionsForMethod(context);
        String currentVersionString = currentVersion.toString();

        if (versionsForThisMethod.contains(currentVersionString)) {
            return ConditionEvaluationResult.enabled(
                    "Gradle version " + currentVersionString + " is in the allowed set for this method");
        }

        return ConditionEvaluationResult.disabled("Gradle version " + currentVersionString
                + " is not in the allowed set for this method: " + versionsForThisMethod);
    }

    private Set<String> getVersionsForMethod(ExtensionContext context) {

        // Add base versions from configuration
        Set<String> versions = new LinkedHashSet<>(GradleVersioningClassTemplate.configuredVersions(context));

        // Add class-level additional versions
        context.getTestClass()
                .flatMap(testClass -> AnnotationSupport.findAnnotation(testClass, AdditionalGradleVersions.class))
                .map(AdditionalGradleVersions::value)
                .ifPresent(additionalVersions -> versions.addAll(Arrays.asList(additionalVersions)));

        // Add method-level additional versions (only for this method)
        context.getTestMethod()
                .flatMap(method -> AnnotationSupport.findAnnotation(method, AdditionalGradleVersions.class))
                .map(AdditionalGradleVersions::value)
                .ifPresent(additionalVersions -> versions.addAll(Arrays.asList(additionalVersions)));

        return versions;
    }
}
