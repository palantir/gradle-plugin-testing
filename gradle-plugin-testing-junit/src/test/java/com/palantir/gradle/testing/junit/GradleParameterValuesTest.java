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

import com.palantir.example.CartesianProductFixture;
import com.palantir.example.TypeMismatchFixture;
import com.palantir.example.VersionComparisonFixture;
import com.palantir.gradle.testing.execution.GradleVersion;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(GradlePluginTestingDisplayNameGenerator.class)
final class GradleParameterValuesTest {

    @Nested
    class TypeSafety {

        @Test
        void throws_when_forversion_uses_ints_but_otherwise_uses_strings() throws Exception {
            Method method = TypeMismatchFixture.class.getDeclaredMethod(
                    "strings_otherwise_but_ints_forversion", String.class);

            assertThatThrownBy(() -> GradleParameterValues.computeInvocations(method, new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("uses string for otherwise but ForVersion specifies int");
        }

        @Test
        void throws_when_forversion_uses_strings_but_otherwise_uses_ints() throws Exception {
            Method method = TypeMismatchFixture.class.getDeclaredMethod(
                    "ints_otherwise_but_strings_forversion", int.class);

            assertThatThrownBy(() -> GradleParameterValues.computeInvocations(method, new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("uses int for otherwise but ForVersion specifies string");
        }

        @Test
        void throws_when_forversion_specifies_both_strings_and_ints() throws Exception {
            Method method = TypeMismatchFixture.class.getDeclaredMethod("forversion_both_types", String.class);

            assertThatThrownBy(() -> GradleParameterValues.computeInvocations(method, new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ForVersion cannot specify multiple value types");
        }

        @Test
        void throws_when_otherwise_specifies_both_strings_and_ints() throws Exception {
            Method method = TypeMismatchFixture.class.getDeclaredMethod("otherwise_both_types", String.class);

            assertThatThrownBy(() -> GradleParameterValues.computeInvocations(method, new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("cannot specify multiple otherwise value types");
        }

        @Test
        void throws_when_forversion_has_neither_equalto_nor_lessthan() throws Exception {
            Method method = TypeMismatchFixture.class.getDeclaredMethod("forversion_no_condition", String.class);

            assertThatThrownBy(() -> GradleParameterValues.computeInvocations(method, new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must specify either equalTo or lessThan");
        }

        @Test
        void throws_when_forversion_has_both_equalto_and_lessthan() throws Exception {
            Method method = TypeMismatchFixture.class.getDeclaredMethod("forversion_both_conditions", String.class);

            assertThatThrownBy(() -> GradleParameterValues.computeInvocations(method, new GradleVersion("8.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("cannot specify both equalTo and lessThan");
        }

        @Test
        void throws_when_no_matching_values_and_no_otherwise() throws Exception {
            Method method = TypeMismatchFixture.class.getDeclaredMethod("no_otherwise_no_match", String.class);

            // Version 10.0 doesn't match lessThan 9.0 and there's no otherwise
            assertThatThrownBy(() -> GradleParameterValues.computeInvocations(method, new GradleVersion("10.0")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No matching values found");
        }
    }

    @Nested
    class VersionComparison {

        @Test
        void lessthan_matches_older_versions() throws Exception {
            Method method = VersionComparisonFixture.class.getDeclaredMethod("lessthan_test", String.class);

            // 8.0 < 9.0, should use "old"
            List<Map<String, Object>> invocations =
                    GradleParameterValues.computeInvocations(method, new GradleVersion("8.0"));

            assertThat(invocations).hasSize(1);
            assertThat(invocations.get(0)).containsEntry("value", "old");
        }

        @Test
        void lessthan_does_not_match_equal_version() throws Exception {
            Method method = VersionComparisonFixture.class.getDeclaredMethod("lessthan_test", String.class);

            // 9.0 is NOT < 9.0, should use otherwise ("new")
            List<Map<String, Object>> invocations =
                    GradleParameterValues.computeInvocations(method, new GradleVersion("9.0"));

            assertThat(invocations).hasSize(1);
            assertThat(invocations.get(0)).containsEntry("value", "new");
        }

        @Test
        void lessthan_does_not_match_newer_version() throws Exception {
            Method method = VersionComparisonFixture.class.getDeclaredMethod("lessthan_test", String.class);

            // 10.0 is NOT < 9.0, should use otherwise ("new")
            List<Map<String, Object>> invocations =
                    GradleParameterValues.computeInvocations(method, new GradleVersion("10.0"));

            assertThat(invocations).hasSize(1);
            assertThat(invocations.get(0)).containsEntry("value", "new");
        }

        @Test
        void equalto_matches_exact_version() throws Exception {
            Method method = VersionComparisonFixture.class.getDeclaredMethod("equalto_test", String.class);

            // 8.14.3 == 8.14.3, should use "special"
            List<Map<String, Object>> invocations =
                    GradleParameterValues.computeInvocations(method, new GradleVersion("8.14.3"));

            assertThat(invocations).hasSize(1);
            assertThat(invocations.get(0)).containsEntry("value", "special");
        }

        @Test
        void equalto_does_not_match_different_version() throws Exception {
            Method method = VersionComparisonFixture.class.getDeclaredMethod("equalto_test", String.class);

            // 8.14.2 != 8.14.3, should use otherwise ("default")
            List<Map<String, Object>> invocations =
                    GradleParameterValues.computeInvocations(method, new GradleVersion("8.14.2"));

            assertThat(invocations).hasSize(1);
            assertThat(invocations.get(0)).containsEntry("value", "default");
        }

        @Test
        void overlapping_conditions_combine_values() throws Exception {
            Method method = VersionComparisonFixture.class.getDeclaredMethod("overlapping_test", String.class);

            // 8.14.3 matches both lessThan 9.0 AND equalTo 8.14.3
            // Should get both "lessThan" and "equalTo"
            List<Map<String, Object>> invocations =
                    GradleParameterValues.computeInvocations(method, new GradleVersion("8.14.3"));

            assertThat(invocations).hasSize(2);
            assertThat(invocations).extracting(m -> m.get("value")).containsExactlyInAnyOrder("lessThan", "equalTo");
        }

        @Test
        void version_comparison_handles_different_segment_counts() throws Exception {
            Method method = VersionComparisonFixture.class.getDeclaredMethod("lessthan_test", String.class);

            // 8 (interpreted as 8.0.0) < 9.0
            List<Map<String, Object>> invocations =
                    GradleParameterValues.computeInvocations(method, new GradleVersion("8"));

            assertThat(invocations).hasSize(1);
            assertThat(invocations.get(0)).containsEntry("value", "old");
        }

        @Test
        void version_comparison_handles_prerelease_versions() throws Exception {
            Method method = VersionComparisonFixture.class.getDeclaredMethod("lessthan_test", String.class);

            // 8.5-rc-1 (interpreted as 8.5.x) < 9.0
            List<Map<String, Object>> invocations =
                    GradleParameterValues.computeInvocations(method, new GradleVersion("8.5-rc-1"));

            assertThat(invocations).hasSize(1);
            assertThat(invocations.get(0)).containsEntry("value", "old");
        }
    }

    @Nested
    class CartesianProduct {

        @Test
        void single_parameter_single_value_produces_one_invocation() throws Exception {
            Method method = CartesianProductFixture.class.getDeclaredMethod("single_param_single_value", String.class);

            List<Map<String, Object>> invocations =
                    GradleParameterValues.computeInvocations(method, new GradleVersion("8.0"));

            assertThat(invocations).hasSize(1);
            assertThat(invocations.get(0)).containsEntry("value", "only");
        }

        @Test
        void single_parameter_multiple_values_produces_multiple_invocations() throws Exception {
            Method method =
                    CartesianProductFixture.class.getDeclaredMethod("single_param_multiple_values", String.class);

            List<Map<String, Object>> invocations =
                    GradleParameterValues.computeInvocations(method, new GradleVersion("8.0"));

            assertThat(invocations).hasSize(3);
            assertThat(invocations).extracting(m -> m.get("value")).containsExactly("a", "b", "c");
        }

        @Test
        void multiple_parameters_produces_cartesian_product() throws Exception {
            Method method =
                    CartesianProductFixture.class.getDeclaredMethod("multiple_params", String.class, int.class);

            List<Map<String, Object>> invocations =
                    GradleParameterValues.computeInvocations(method, new GradleVersion("8.0"));

            // 2 string values x 2 int values = 4 invocations
            assertThat(invocations).hasSize(4);
            assertThat(invocations)
                    .containsExactlyInAnyOrder(
                            Map.of("str", "a", "num", 1),
                            Map.of("str", "a", "num", 2),
                            Map.of("str", "b", "num", 1),
                            Map.of("str", "b", "num", 2));
        }
    }
}
