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
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * Execution condition that filters tests based on Gradle version.
 *
 * <p>When a method has its own {@link WithGradleVersions} annotation, this condition ensures
 * that only the method-specific versions (plus base and class-level versions) run for that method.
 * For methods without the annotation, only base and class-level versions run.
 *
 * <p>When a method has {@link WithOnlyGradleVersions}, the allowed versions are filtered
 * to only include versions that appear in both the normal allowed set AND the "only" filter.
 * Class-level {@link WithOnlyGradleVersions} is handled in {@link GradleVersioningClassTemplate}
 * to filter the test matrix upfront.
 */
final class WithGradleVersionsCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        // Only evaluate for test methods
        if (context.getTestMethod().isEmpty()) {
            return ConditionEvaluationResult.enabled("No test method");
        }

        GradleVersion currentVersion = GradleVersionStore.gradleVersion(context);
        String currentVersionString = currentVersion.toString();

        Set<String> versionsForThisMethod = new LinkedHashSet<>(GradleVersioningClassTemplate.baseVersions(context));
        context.getTestMethod()
                .map(GradleVersioningClassTemplate::methodVersions)
                .ifPresent(versionsForThisMethod::addAll);

        // Apply WithOnlyGradleVersions filter if present (class-level first, then method-level)
        Optional<Set<String>> onlyFilter = getOnlyGradleVersionsFilter(context);
        onlyFilter.ifPresent(versionsForThisMethod::retainAll);

        if (versionsForThisMethod.contains(currentVersionString)) {
            return ConditionEvaluationResult.enabled(
                    "Gradle version " + currentVersionString + " is in the allowed set for this method");
        }

        return ConditionEvaluationResult.disabled("Gradle version " + currentVersionString
                + " is not in the allowed set for this method: " + versionsForThisMethod);
    }

    private Optional<Set<String>> getOnlyGradleVersionsFilter(ExtensionContext context) {
        // Only check method-level @WithOnlyGradleVersions here.
        // Class-level is handled in GradleVersioningClassTemplate to filter the matrix upfront.
        return context.getTestMethod()
                .flatMap(method -> AnnotationSupport.findAnnotation(method, WithOnlyGradleVersions.class))
                .map(annotation -> new LinkedHashSet<>(Arrays.asList(annotation.value())));
    }
}
