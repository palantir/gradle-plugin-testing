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
import java.util.Set;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Execution condition that filters tests based on Gradle version.
 *
 * <p>When a method has its own {@link WithSpecialCaseGradleVersions} annotation, this condition ensures
 * that only the method-specific versions (plus base and class-level versions) run for that method.
 * For methods without the annotation, only base and class-level versions run.
 *
 * <p>When a method has filter annotations like {@link RestrictToGradleVersionsEqualTo}, the allowed versions
 * are filtered accordingly. Class-level filter annotations are handled in {@link GradleVersioningClassTemplate}
 * to filter the test matrix upfront.
 */
final class WithSpecialCaseGradleVersionsCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (context.getTestMethod().isEmpty()) {
            return ConditionEvaluationResult.enabled("No test method");
        }

        GradleVersion currentVersion = GradleVersionStore.gradleVersion(context);
        Set<GradleVersion> versionsForThisMethod = GradleVersions.filteredVersionsForMethod(context);

        if (versionsForThisMethod.contains(currentVersion)) {
            return ConditionEvaluationResult.enabled(
                    "Gradle version " + currentVersion + " is in the allowed set for this method");
        }

        return ConditionEvaluationResult.disabled("Gradle version " + currentVersion
                + " is not in the allowed set for this method: " + versionsForThisMethod);
    }
}
