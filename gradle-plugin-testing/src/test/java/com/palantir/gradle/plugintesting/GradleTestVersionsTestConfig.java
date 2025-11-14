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

package com.palantir.gradle.plugintesting;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class GradleTestVersionsTestConfig {

    @Nested
    class Deserialize {
        private static final String NORMAL_YAML = """
            major-versions:
              8: 8.14.2
              9: 9.2.0
            extra-versions:
              - 8.8
            """;
        private static final String RC_YAML = """
            major-versions:
              8: 8.14.2
              9: 9.2.0-rc1
            extra-versions:
              - 8.8
            """;
        private static final String NO_EXTRA_VERSIONS_YAML = """
            major-versions:
              8: 8.14.2
              9: 9.2.0
            """;
        private static final String EXTRA_VERSIONS_ONLY_YAML = """
            extra-versions:
              - 8.8
              - 8.9
              - 9.0
            """;

        @TempDir
        Path tempDir;

        @Test
        public void deserialize_ok() throws IOException {
            Path yamlFile = tempDir.resolve("versions.yaml");
            Files.writeString(yamlFile, NORMAL_YAML);

            GradleTestVersionsConfig config = GradleTestVersionsConfig.deserialize(yamlFile);

            assertThat(config.majorVersions()).containsEntry(8, "8.14.2").containsEntry(9, "9.2.0");
            assertThat(config.extraVersions()).containsExactly("8.8");
        }

        @Test
        public void deserialize_without_extra_versions_ok() throws IOException {
            Path yamlFile = tempDir.resolve("versions.yaml");
            Files.writeString(yamlFile, NO_EXTRA_VERSIONS_YAML);

            GradleTestVersionsConfig config = GradleTestVersionsConfig.deserialize(yamlFile);

            assertThat(config.majorVersions()).containsEntry(8, "8.14.2").containsEntry(9, "9.2.0");
            assertThat(config.extraVersions()).isEmpty();
        }

        @Test
        public void deserialize_with_rc_ok() throws IOException {
            Path yamlFile = tempDir.resolve("versions.yaml");
            Files.writeString(yamlFile, RC_YAML);

            GradleTestVersionsConfig config = GradleTestVersionsConfig.deserialize(yamlFile);

            assertThat(config.majorVersions()).containsEntry(8, "8.14.2").containsEntry(9, "9.2.0-rc1");
            assertThat(config.extraVersions()).containsExactly("8.8");
        }

        @Test
        public void deserialize_empty_yaml_ok() throws IOException {
            Path yamlFile = tempDir.resolve("empty.yaml");
            Files.writeString(yamlFile, "");

            GradleTestVersionsConfig config = GradleTestVersionsConfig.deserialize(yamlFile);

            assertThat(config.majorVersions()).isEmpty();
            assertThat(config.extraVersions()).isEmpty();
        }

        @Test
        public void deserialize_extra_versions_only_ok() throws IOException {
            Path yamlFile = tempDir.resolve("extra-only.yaml");
            Files.writeString(yamlFile, EXTRA_VERSIONS_ONLY_YAML);

            GradleTestVersionsConfig config = GradleTestVersionsConfig.deserialize(yamlFile);

            assertThat(config.majorVersions()).isEmpty();
            assertThat(config.extraVersions()).containsExactly("8.8", "8.9", "9.0");
        }
    }

    @Nested
    class WithMajorVersion {
        @Test
        public void add_new_major_version() {
            GradleTestVersionsConfig config = GradleTestVersionsConfig.builder()
                    .putMajorVersions(7, "7.6.4")
                    .putMajorVersions(8, "8.10.0")
                    .build();

            GradleTestVersionsConfig updated = config.withMajorVersion("9.2.0");

            assertThat(updated.majorVersions())
                    .containsEntry(7, "7.6.4")
                    .containsEntry(8, "8.10.0")
                    .containsEntry(9, "9.2.0");
        }

        @Test
        public void replace_existing_major_version() {
            GradleTestVersionsConfig config = GradleTestVersionsConfig.builder()
                    .putMajorVersions(8, "8.10.0")
                    .build();

            GradleTestVersionsConfig updated = config.withMajorVersion("8.14.2");

            assertThat(updated.majorVersions()).containsEntry(8, "8.14.2");
            assertThat(updated.majorVersions()).hasSize(1);
        }
    }

    @Nested
    class WithoutMajorVersion {
        @Test
        public void removes_major_version() {
            GradleTestVersionsConfig config = GradleTestVersionsConfig.builder()
                    .putMajorVersions(7, "7.6.4")
                    .putMajorVersions(8, "8.14.2")
                    .putMajorVersions(9, "9.2.0")
                    .build();

            GradleTestVersionsConfig updated = config.withoutMajorVersion(8);

            assertThat(updated.majorVersions())
                    .containsEntry(7, "7.6.4")
                    .containsEntry(9, "9.2.0")
                    .doesNotContainKey(8);
        }

        @Test
        public void removes_matching_extra_versions() {
            GradleTestVersionsConfig config = GradleTestVersionsConfig.builder()
                    .putMajorVersions(8, "8.14.2")
                    .putMajorVersions(9, "9.2.0")
                    .addExtraVersions("8.8")
                    .addExtraVersions("8.10")
                    .addExtraVersions("9.0")
                    .build();

            GradleTestVersionsConfig updated = config.withoutMajorVersion(8);

            assertThat(updated.majorVersions()).doesNotContainKey(8);
            assertThat(updated.extraVersions()).containsExactly("9.0");
        }

        @Test
        public void keeps_unrelated_extra_versions() {
            GradleTestVersionsConfig config = GradleTestVersionsConfig.builder()
                    .putMajorVersions(7, "7.6.4")
                    .putMajorVersions(8, "8.14.2")
                    .addExtraVersions("7.5")
                    .addExtraVersions("8.8")
                    .build();

            GradleTestVersionsConfig updated = config.withoutMajorVersion(8);

            assertThat(updated.majorVersions()).containsEntry(7, "7.6.4");
            assertThat(updated.extraVersions()).containsExactly("7.5");
        }

        @Test
        public void handles_non_existent_major_versions() {
            GradleTestVersionsConfig config = GradleTestVersionsConfig.builder()
                    .putMajorVersions(8, "8.14.2")
                    .build();

            GradleTestVersionsConfig updated = config.withoutMajorVersion(9);

            assertThat(updated.majorVersions()).containsEntry(8, "8.14.2");
            assertThat(updated.majorVersions()).hasSize(1);
        }
    }

    @Nested
    class Miscellaneous {
        @Test
        public void testBuilderPattern() {
            GradleTestVersionsConfig config = GradleTestVersionsConfig.builder()
                    .majorVersions(java.util.Map.of(8, "8.14.2", 9, "9.2.0"))
                    .extraVersions(java.util.List.of("8.8"))
                    .build();

            assertThat(config.majorVersions()).containsEntry(8, "8.14.2").containsEntry(9, "9.2.0");
            assertThat(config.extraVersions()).containsExactly("8.8");
        }

        @Test
        public void testEqualsAndHashCode() {
            GradleTestVersionsConfig config1 = GradleTestVersionsConfig.builder()
                    .majorVersions(java.util.Map.of(8, "8.14.2"))
                    .extraVersions(java.util.List.of("8.8"))
                    .build();

            GradleTestVersionsConfig config2 = GradleTestVersionsConfig.builder()
                    .majorVersions(java.util.Map.of(8, "8.14.2"))
                    .extraVersions(java.util.List.of("8.8"))
                    .build();

            assertThat(config1).isEqualTo(config2);
            assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
        }
    }
}
