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

import com.palantir.example.ParameterizedByGradleVersionValuesFixtures.InvalidFixtures;
import com.palantir.example.ParameterizedByGradleVersionValuesFixtures.ValidFixtures;
import com.palantir.gradle.testing.execution.GradleVersion;
import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class ParameterizedByGradleVersionValuesTest {

    @Nested
    class ValidConditions {

        @Test
        void single_when_condition() throws Exception {
            Method method = ValidFixtures.class.getDeclaredMethod("singleCondition", String.class);

            assertThat(ParameterizedByGradleVersionValues.computeValue(method, "behaviour", new GradleVersion("7.6.4")))
                    .isEqualTo(Optional.of("old"));
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, "behaviour", new GradleVersion("7.9.9")))
                    .isEqualTo(Optional.of("old"));
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, "behaviour", new GradleVersion("8.0")))
                    .isEqualTo(Optional.of("new"));
            assertThat(ParameterizedByGradleVersionValues.computeValue(
                            method, "behaviour", new GradleVersion("8.14.3")))
                    .isEqualTo(Optional.of("new"));
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, "behaviour", new GradleVersion("9.0")))
                    .isEqualTo(Optional.of("new"));
        }

        @Test
        void two_when_conditions() throws Exception {
            Method method = ValidFixtures.class.getDeclaredMethod("twoConditions", String.class);

            assertThat(ParameterizedByGradleVersionValues.computeValue(method, "behaviour", new GradleVersion("7.6.4")))
                    .isEqualTo(Optional.of("less than 8"));
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, "behaviour", new GradleVersion("8.0")))
                    .isEqualTo(Optional.of("8.x"));
            assertThat(ParameterizedByGradleVersionValues.computeValue(
                            method, "behaviour", new GradleVersion("8.14.3")))
                    .isEqualTo(Optional.of("8.x"));
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, "behaviour", new GradleVersion("9.0")))
                    .isEqualTo(Optional.of("9 and up"));
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, "behaviour", new GradleVersion("10.0")))
                    .isEqualTo(Optional.of("9 and up"));
        }

        @Test
        void no_annotation_returns_empty() throws Exception {
            Method method = ValidFixtures.class.getDeclaredMethod("noAnnotation");

            assertThat(ParameterizedByGradleVersionValues.computeValue(method, "behaviour", new GradleVersion("8.0")))
                    .isEmpty();
        }

        @Test
        void single_annotation_without_name_matches_any_parameter() throws Exception {
            Method method = ValidFixtures.class.getDeclaredMethod("singleCondition", String.class);

            // Single annotation without name matches any @InjectByGradleVersion parameter regardless of its name
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, "anyName", new GradleVersion("8.0")))
                    .isEqualTo(Optional.of("new"));
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, "anotherName", new GradleVersion("7.0")))
                    .isEqualTo(Optional.of("old"));
        }

        @Test
        void multiple_annotations_unmatched_name_returns_empty() throws Exception {
            Method method = ValidFixtures.class.getDeclaredMethod("twoAnnotations", String.class, String.class);

            // When there are multiple annotations, name must match
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, "wrongName", new GradleVersion("8.0")))
                    .isEmpty();
        }

        @Test
        void multiple_annotations_with_different_names() throws Exception {
            Method method = ValidFixtures.class.getDeclaredMethod("twoAnnotations", String.class, String.class);

            assertThat(ParameterizedByGradleVersionValues.computeValue(method, "first", new GradleVersion("7.0")))
                    .isEqualTo(Optional.of("old"));
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, "first", new GradleVersion("8.0")))
                    .isEqualTo(Optional.of("new"));

            assertThat(ParameterizedByGradleVersionValues.computeValue(method, "second", new GradleVersion("8.0")))
                    .isEqualTo(Optional.of("before 9"));
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, "second", new GradleVersion("9.0")))
                    .isEqualTo(Optional.of("after 9"));
        }
    }

    @Nested
    class InvalidOrdering {

        @Test
        void descending_order_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("descendingOrder", String.class);

            assertThatThrownBy(() -> ParameterizedByGradleVersionValues.computeValue(
                            method, "behaviour", new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must have @WhenVersion conditions ordered by ascending lessThan version");
        }

        @Test
        void duplicate_versions_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("duplicateVersions", String.class);

            assertThatThrownBy(() -> ParameterizedByGradleVersionValues.computeValue(
                            method, "behaviour", new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must have @WhenVersion conditions ordered by ascending lessThan version");
        }

        @Test
        void out_of_order_in_middle_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("outOfOrderMiddle", String.class);

            assertThatThrownBy(() -> ParameterizedByGradleVersionValues.computeValue(
                            method, "behaviour", new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must have @WhenVersion conditions ordered by ascending lessThan version");
        }

        @Test
        void duplicate_names_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("duplicateNames", String.class, String.class);

            assertThatThrownBy(() -> ParameterizedByGradleVersionValues.computeValue(
                            method, "behaviour", new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("has duplicate name values: [behaviour]");
        }

        @Test
        void missing_name_when_multiple_annotations_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("missingNameOnSecond", String.class, String.class);

            assertThatThrownBy(() ->
                            ParameterizedByGradleVersionValues.computeValue(method, "first", new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("name is required when multiple annotations are present");
        }

        @Test
        void missing_inject_parameter_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("missingInjectParameter");

            assertThatThrownBy(() -> ParameterizedByGradleVersionValues.computeValue(
                            method, "behaviour", new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("without a name requires exactly one")
                    .hasMessageContaining("@InjectByGradleVersion parameter (found 0)");
        }

        @Test
        void two_annotations_but_only_one_parameter_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("twoAnnotationsButOnlyOneParameter", String.class);

            assertThatThrownBy(() ->
                            ParameterizedByGradleVersionValues.computeValue(method, "first", new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no @InjectByGradleVersion parameter found for name(s):")
                    .hasMessageContaining("second");
        }

        @Test
        void extra_inject_parameter_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("extraInjectParameter", String.class, String.class);

            assertThatThrownBy(() -> ParameterizedByGradleVersionValues.computeValue(
                            method, "behaviour", new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("without a name requires exactly one")
                    .hasMessageContaining("@InjectByGradleVersion parameter (found 2)");
        }

        @Test
        void mismatched_annotation_name_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("mismatchedAnnotationName", String.class);

            assertThatThrownBy(() ->
                            ParameterizedByGradleVersionValues.computeValue(method, "param", new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no @InjectByGradleVersion parameter found for name(s):")
                    .hasMessageContaining("wrongName");
        }

        @Test
        void extra_parameter_with_wrong_name_throws() throws Exception {
            Method method =
                    InvalidFixtures.class.getDeclaredMethod("extraParameterWithWrongName", String.class, String.class);

            assertThatThrownBy(() ->
                            ParameterizedByGradleVersionValues.computeValue(method, "first", new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no annotation found for @InjectByGradleVersion parameter(s):")
                    .hasMessageContaining("extra");
        }
    }
}
