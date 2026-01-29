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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.palantir.gradle.testing.execution.GradleVersion;
import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class ParameterizedByGradleVersionValuesTest {

    @Nested
    class ValidConditions {

        @Test
        void no_when_conditions_returns_otherwise() throws Exception {
            Method method = ValidFixtures.class.getDeclaredMethod("noConditions");

            assertThat(ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("7.0")))
                    .isEqualTo(Optional.of("default"));
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("8.0")))
                    .isEqualTo(Optional.of("default"));
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("9.0")))
                    .isEqualTo(Optional.of("default"));
        }

        @Test
        void single_when_condition() throws Exception {
            Method method = ValidFixtures.class.getDeclaredMethod("singleCondition");

            assertThat(ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("7.6.4")))
                    .isEqualTo(Optional.of("old"));
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("7.9.9")))
                    .isEqualTo(Optional.of("old"));
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("8.0")))
                    .isEqualTo(Optional.of("new"));
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("8.14.3")))
                    .isEqualTo(Optional.of("new"));
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("9.0")))
                    .isEqualTo(Optional.of("new"));
        }

        @Test
        void two_when_conditions() throws Exception {
            Method method = ValidFixtures.class.getDeclaredMethod("twoConditions");

            assertThat(ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("7.6.4")))
                    .isEqualTo(Optional.of("less than 8"));
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("8.0")))
                    .isEqualTo(Optional.of("8.x"));
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("8.14.3")))
                    .isEqualTo(Optional.of("8.x"));
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("9.0")))
                    .isEqualTo(Optional.of("9 and up"));
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("10.0")))
                    .isEqualTo(Optional.of("9 and up"));
        }

        @Test
        void no_annotation_returns_empty() throws Exception {
            Method method = ValidFixtures.class.getDeclaredMethod("noAnnotation");

            assertThat(ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("8.0")))
                    .isEmpty();
        }

        @Test
        void parameter_name_returns_correct_value() throws Exception {
            Method method = ValidFixtures.class.getDeclaredMethod("singleCondition");

            assertThat(ParameterizedByGradleVersionValues.parameterName(method)).isEqualTo(Optional.of("behaviour"));
        }

        @Test
        void parameter_name_returns_empty_when_no_annotation() throws Exception {
            Method method = ValidFixtures.class.getDeclaredMethod("noAnnotation");

            assertThat(ParameterizedByGradleVersionValues.parameterName(method)).isEmpty();
        }
    }

    @Nested
    class InvalidOrdering {

        @Test
        void descending_order_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("descendingOrder");

            assertThatThrownBy(() -> ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must have @WhenVersion conditions ordered by ascending lessThan version");
        }

        @Test
        void duplicate_versions_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("duplicateVersions");

            assertThatThrownBy(() -> ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must have @WhenVersion conditions ordered by ascending lessThan version");
        }

        @Test
        void out_of_order_in_middle_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("outOfOrderMiddle");

            assertThatThrownBy(() -> ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must have @WhenVersion conditions ordered by ascending lessThan version");
        }
    }

    // Test fixture classes with annotations for testing
    static class ValidFixtures {

        @ParameterizedByGradleVersion(
                name = "behaviour",
                otherwiseString = "default",
                when = {})
        void noConditions() {}

        @ParameterizedByGradleVersion(
                name = "behaviour",
                otherwiseString = "new",
                when = @WhenVersion(lessThan = "8.0", stringValue = "old"))
        void singleCondition() {}

        @ParameterizedByGradleVersion(
                name = "behaviour",
                otherwiseString = "9 and up",
                when = {
                    @WhenVersion(lessThan = "8.0", stringValue = "less than 8"),
                    @WhenVersion(lessThan = "9.0", stringValue = "8.x")
                })
        void twoConditions() {}

        void noAnnotation() {}
    }

    static class InvalidFixtures {

        @ParameterizedByGradleVersion(
                name = "behaviour",
                otherwiseString = "default",
                when = {
                    @WhenVersion(lessThan = "9.0", stringValue = "a"),
                    @WhenVersion(lessThan = "8.0", stringValue = "b")
                })
        void descendingOrder() {}

        @ParameterizedByGradleVersion(
                name = "behaviour",
                otherwiseString = "default",
                when = {
                    @WhenVersion(lessThan = "8.0", stringValue = "a"),
                    @WhenVersion(lessThan = "8.0", stringValue = "b")
                })
        void duplicateVersions() {}

        @ParameterizedByGradleVersion(
                name = "behaviour",
                otherwiseString = "default",
                when = {
                    @WhenVersion(lessThan = "7.0", stringValue = "a"),
                    @WhenVersion(lessThan = "9.0", stringValue = "b"),
                    @WhenVersion(lessThan = "8.0", stringValue = "c")
                })
        void outOfOrderMiddle() {}
    }
}
