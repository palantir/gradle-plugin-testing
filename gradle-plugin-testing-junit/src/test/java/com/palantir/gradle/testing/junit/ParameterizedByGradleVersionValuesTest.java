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
    class ValidRanges {

        @Test
        void single_range_covering_all_versions() throws Exception {
            Method method = ValidFixtures.class.getDeclaredMethod("singleRange");

            assertThat(ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("7.0")))
                    .isEqualTo(Optional.of("all"));
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("8.0")))
                    .isEqualTo(Optional.of("all"));
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("9.0")))
                    .isEqualTo(Optional.of("all"));
        }

        @Test
        void two_ranges_split_at_8() throws Exception {
            Method method = ValidFixtures.class.getDeclaredMethod("twoRanges");

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
        void three_ranges() throws Exception {
            Method method = ValidFixtures.class.getDeclaredMethod("threeRanges");

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
        void boundary_version_goes_to_higher_range() throws Exception {
            Method method = ValidFixtures.class.getDeclaredMethod("twoRanges");

            // lowerBound is inclusive, upperBound is exclusive
            // so 8.0 exactly should match the "new" range (lowerBound = "8.0")
            assertThat(ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("8.0")))
                    .isEqualTo(Optional.of("new"));
        }

        @Test
        void no_annotations_returns_empty() throws Exception {
            Method method = ValidFixtures.class.getDeclaredMethod("noAnnotations");

            assertThat(ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("8.0")))
                    .isEmpty();
        }
    }

    @Nested
    class InvalidRanges {

        @Test
        void missing_lower_range_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("missingLowerRange");

            assertThatThrownBy(() -> ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must have contiguous ranges covering all versions");
        }

        @Test
        void missing_upper_range_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("missingUpperRange");

            assertThatThrownBy(() -> ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must have contiguous ranges covering all versions");
        }

        @Test
        void gap_between_ranges_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("gapBetweenRanges");

            assertThatThrownBy(() -> ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must have contiguous ranges covering all versions");
        }

        @Test
        void overlap_between_ranges_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("overlapBetweenRanges");

            assertThatThrownBy(() -> ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must have contiguous ranges covering all versions");
        }

        @Test
        void unbounded_range_in_middle_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("unboundedMiddle");

            assertThatThrownBy(() -> ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must have contiguous ranges covering all versions");
        }

        @Test
        void duplicate_upper_bounds_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("duplicateUpperBounds");

            assertThatThrownBy(() -> ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must have contiguous ranges covering all versions");
        }

        @Test
        void duplicate_lower_bounds_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("duplicateLowerBounds");

            assertThatThrownBy(() -> ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must have contiguous ranges covering all versions");
        }

        @Test
        void empty_range_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("emptyRange");

            assertThatThrownBy(() -> ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must have contiguous ranges covering all versions");
        }

        @Test
        void inverted_bounds_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("invertedBounds");

            assertThatThrownBy(() -> ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must have contiguous ranges covering all versions");
        }

        @Test
        void single_bounded_range_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("singleBoundedRange");

            assertThatThrownBy(() -> ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("7.5")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must have contiguous ranges covering all versions");
        }

        @Test
        void duplicate_fully_unbounded_throws() throws Exception {
            Method method = InvalidFixtures.class.getDeclaredMethod("duplicateFullyUnbounded");

            assertThatThrownBy(() -> ParameterizedByGradleVersionValues.computeValue(method, new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must have contiguous ranges covering all versions");
        }
    }

    // Test fixture classes with annotations for testing
    static class ValidFixtures {

        @ParameterizedByGradleVersion(stringValue = "all")
        void singleRange() {}

        @ParameterizedByGradleVersion(upperBound = "8.0", stringValue = "old")
        @ParameterizedByGradleVersion(lowerBound = "8.0", stringValue = "new")
        void twoRanges() {}

        @ParameterizedByGradleVersion(upperBound = "8.0", stringValue = "less than 8")
        @ParameterizedByGradleVersion(lowerBound = "8.0", upperBound = "9.0", stringValue = "8.x")
        @ParameterizedByGradleVersion(lowerBound = "9.0", stringValue = "9 and up")
        void threeRanges() {}

        void noAnnotations() {}
    }

    static class InvalidFixtures {

        // Missing range from 0.0.0 to 8.0
        @ParameterizedByGradleVersion(lowerBound = "8.0", stringValue = "new")
        void missingLowerRange() {}

        // Missing range from 9.0 to infinity
        @ParameterizedByGradleVersion(upperBound = "9.0", stringValue = "old")
        void missingUpperRange() {}

        // Gap between 7.0 and 8.0
        @ParameterizedByGradleVersion(upperBound = "7.0", stringValue = "old")
        @ParameterizedByGradleVersion(lowerBound = "8.0", stringValue = "new")
        void gapBetweenRanges() {}

        // Overlap: first ends at 8.5, second starts at 8.0
        @ParameterizedByGradleVersion(upperBound = "8.5", stringValue = "old")
        @ParameterizedByGradleVersion(lowerBound = "8.0", stringValue = "new")
        void overlapBetweenRanges() {}

        // Middle range has no upper bound
        @ParameterizedByGradleVersion(upperBound = "7.0", stringValue = "old")
        @ParameterizedByGradleVersion(lowerBound = "7.0", stringValue = "middle")
        @ParameterizedByGradleVersion(lowerBound = "9.0", stringValue = "new")
        void unboundedMiddle() {}

        // Duplicate upper bounds
        @ParameterizedByGradleVersion(upperBound = "8.0", stringValue = "old")
        @ParameterizedByGradleVersion(upperBound = "8.0", stringValue = "old2")
        @ParameterizedByGradleVersion(lowerBound = "8.0", stringValue = "new")
        void duplicateUpperBounds() {}

        // Duplicate lower bounds
        @ParameterizedByGradleVersion(upperBound = "8.0", stringValue = "old")
        @ParameterizedByGradleVersion(lowerBound = "8.0", stringValue = "new")
        @ParameterizedByGradleVersion(lowerBound = "8.0", stringValue = "new2")
        void duplicateLowerBounds() {}

        // Empty range: lowerBound == upperBound
        @ParameterizedByGradleVersion(upperBound = "8.0", stringValue = "old")
        @ParameterizedByGradleVersion(lowerBound = "8.0", upperBound = "8.0", stringValue = "empty")
        @ParameterizedByGradleVersion(lowerBound = "8.0", stringValue = "new")
        void emptyRange() {}

        // Inverted bounds: lowerBound > upperBound
        @ParameterizedByGradleVersion(upperBound = "8.0", stringValue = "old")
        @ParameterizedByGradleVersion(lowerBound = "9.0", upperBound = "8.0", stringValue = "inverted")
        @ParameterizedByGradleVersion(lowerBound = "8.0", stringValue = "new")
        void invertedBounds() {}

        // Single bounded range not covering all versions
        @ParameterizedByGradleVersion(lowerBound = "7.0", upperBound = "8.0", stringValue = "middle")
        void singleBoundedRange() {}

        // Two fully unbounded annotations
        @ParameterizedByGradleVersion(stringValue = "all1")
        @ParameterizedByGradleVersion(stringValue = "all2")
        void duplicateFullyUnbounded() {}
    }
}
