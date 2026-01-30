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
import com.palantir.gradle.testing.junit.ParameterizedByGradleVersion.WhenVersion;
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

            assertThat(ParameterizedByGradleVersionValues.computeValue(method, "behaviour", new GradleVersion("7.0")))
                    .isEqualTo(Optional.of("default"));
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, "behaviour", new GradleVersion("8.0")))
                    .isEqualTo(Optional.of("default"));
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, "behaviour", new GradleVersion("9.0")))
                    .isEqualTo(Optional.of("default"));
        }

        @Test
        void single_when_condition() throws Exception {
            Method method = ValidFixtures.class.getDeclaredMethod("singleCondition");

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
            Method method = ValidFixtures.class.getDeclaredMethod("twoConditions");

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
            Method method = ValidFixtures.class.getDeclaredMethod("singleCondition");

            // Single annotation without name matches any @InjectByGradleVersion parameter regardless of its name
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, "anyName", new GradleVersion("8.0")))
                    .isEqualTo(Optional.of("new"));
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, "anotherName", new GradleVersion("7.0")))
                    .isEqualTo(Optional.of("old"));
        }

        @Test
        void multiple_annotations_unmatched_name_returns_empty() throws Exception {
            Method method = ValidFixtures.class.getDeclaredMethod("twoAnnotations");

            // When there are multiple annotations, name must match
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, "wrongName", new GradleVersion("8.0")))
                    .isEmpty();
        }

        @Test
        void multiple_annotations_with_different_names() throws Exception {
            Method method = ValidFixtures.class.getDeclaredMethod("twoAnnotations");

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
            Method method = InvalidFixtures.class.getDeclaredMethod("descendingOrder");

            assertThatThrownBy(() -> ParameterizedByGradleVersionValues.computeValue(
                            method, "behaviour", new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must have @WhenVersion conditions ordered by ascending lessThan version");
        }

        @Test
        void duplicate_versions_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("duplicateVersions");

            assertThatThrownBy(() -> ParameterizedByGradleVersionValues.computeValue(
                            method, "behaviour", new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must have @WhenVersion conditions ordered by ascending lessThan version");
        }

        @Test
        void out_of_order_in_middle_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("outOfOrderMiddle");

            assertThatThrownBy(() -> ParameterizedByGradleVersionValues.computeValue(
                            method, "behaviour", new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must have @WhenVersion conditions ordered by ascending lessThan version");
        }

        @Test
        void duplicate_names_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("duplicateNames");

            assertThatThrownBy(() -> ParameterizedByGradleVersionValues.computeValue(
                            method, "behaviour", new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("has duplicate name values");
        }

        @Test
        void missing_name_when_multiple_annotations_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("missingNameOnSecond");

            assertThatThrownBy(() ->
                            ParameterizedByGradleVersionValues.computeValue(method, "first", new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("name is required when multiple annotations are present");
        }
    }

    // Test fixture classes with annotations for testing
    static class ValidFixtures {

        @ParameterizedByGradleVersion(otherwiseString = "default")
        void noConditions() {}

        @ParameterizedByGradleVersion(
                when = @WhenVersion(lessThan = "8.0", stringValue = "old"),
                otherwiseString = "new")
        void singleCondition() {}

        @ParameterizedByGradleVersion(
                when = {
                    @WhenVersion(lessThan = "8.0", stringValue = "less than 8"),
                    @WhenVersion(lessThan = "9.0", stringValue = "8.x")
                },
                otherwiseString = "9 and up")
        void twoConditions() {}

        @ParameterizedByGradleVersion(
                name = "first",
                when = @WhenVersion(lessThan = "8.0", stringValue = "old"),
                otherwiseString = "new")
        @ParameterizedByGradleVersion(
                name = "second",
                when = @WhenVersion(lessThan = "9.0", stringValue = "before 9"),
                otherwiseString = "after 9")
        void twoAnnotations() {}

        void noAnnotation() {}
    }

    static class InvalidFixtures {

        @ParameterizedByGradleVersion(
                when = {
                    @WhenVersion(lessThan = "9.0", stringValue = "a"),
                    @WhenVersion(lessThan = "8.0", stringValue = "b")
                },
                otherwiseString = "default")
        void descendingOrder() {}

        @ParameterizedByGradleVersion(
                when = {
                    @WhenVersion(lessThan = "8.0", stringValue = "a"),
                    @WhenVersion(lessThan = "8.0", stringValue = "b")
                },
                otherwiseString = "default")
        void duplicateVersions() {}

        @ParameterizedByGradleVersion(
                when = {
                    @WhenVersion(lessThan = "7.0", stringValue = "a"),
                    @WhenVersion(lessThan = "9.0", stringValue = "b"),
                    @WhenVersion(lessThan = "8.0", stringValue = "c")
                },
                otherwiseString = "default")
        void outOfOrderMiddle() {}

        @ParameterizedByGradleVersion(
                name = "behaviour",
                when = @WhenVersion(lessThan = "8.0", stringValue = "old"),
                otherwiseString = "first")
        @ParameterizedByGradleVersion(
                name = "behaviour",
                when = @WhenVersion(lessThan = "9.0", stringValue = "also old"),
                otherwiseString = "second")
        void duplicateNames() {}

        @ParameterizedByGradleVersion(
                name = "first",
                when = @WhenVersion(lessThan = "8.0", stringValue = "old"),
                otherwiseString = "new")
        @ParameterizedByGradleVersion(
                when = @WhenVersion(lessThan = "9.0", stringValue = "before 9"),
                otherwiseString = "after 9")
        void missingNameOnSecond() {}
    }
}
