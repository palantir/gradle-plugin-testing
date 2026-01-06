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

package com.palantir.gradle.testing.ete;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.testkit.engine.Event;

public final class Assertions {

    public static void assertThatTestFailureExceptionMessageContains(
            TestExecutionResult testExecutionResult, String exceptionFragment) {
        assertThat(testExecutionResult.getThrowable()).hasValueSatisfying(throwable -> {
            assertThat(throwable).hasMessageContaining(exceptionFragment);
        });
    }

    public static void assertThatRanWithCorrectGradleVersion(Class<?> testClass, Event event, String gradleVersion) {
        assertThatRanWithCorrectGradleVersion(testClass, event, gradleVersion, "test name");
    }

    public static void assertThatRanWithCorrectGradleVersion(
            Class<?> testClass, Event event, String gradleVersion, String testName) {
        assertThatTestContainerDescriptorHasDisplayName(event, "Gradle " + gradleVersion);

        assertThat(event.getPayload(TestExecutionResult.class)).hasValueSatisfying(testExecutionResult -> {
            assertThat(testExecutionResult.getStatus()).isEqualTo(Status.FAILED);

            Assertions.assertThatTestFailureExceptionMessageContains(
                    testExecutionResult, "GradleVersion: " + gradleVersion);
        });

        assertThat(Path.of(
                        "build/gradle-plugin-testing",
                        testClass.getSimpleName(),
                        testName,
                        gradleVersion,
                        "build.gradle"))
                .exists();
    }

    private static void assertThatTestContainerDescriptorHasDisplayName(
            Event event, String containerDescriptorDisplayName) {
        assertThat(event.getTestDescriptor().getParent()).hasValueSatisfying(desc -> {
            assertThat(desc.getDisplayName()).isEqualTo(containerDescriptorDisplayName);
        });
    }

    private Assertions() {}
}
