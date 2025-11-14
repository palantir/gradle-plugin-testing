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
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class GradleTestVersionsConfigTest {

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
            SortedMap<Integer, String> majorVersions = new TreeMap<>();
            majorVersions.put(7, "7.6.4");
            majorVersions.put(8, "8.10.0");

            GradleTestVersionsConfig config = GradleTestVersionsConfig.builder()
                    .majorVersions(majorVersions)
                    .extraVersions(new TreeSet<>())
                    .build();

            GradleTestVersionsConfig updated = config.withMajorVersion("9.2.0");

            assertThat(updated.majorVersions())
                    .containsEntry(7, "7.6.4")
                    .containsEntry(8, "8.10.0")
                    .containsEntry(9, "9.2.0");
        }

        @Test
        public void replace_existing_major_version() {
            SortedMap<Integer, String> majorVersions = new TreeMap<>();
            majorVersions.put(8, "8.10.0");

            GradleTestVersionsConfig config = GradleTestVersionsConfig.builder()
                    .majorVersions(majorVersions)
                    .extraVersions(new TreeSet<>())
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
            SortedMap<Integer, String> majorVersions = new TreeMap<>();
            majorVersions.put(7, "7.6.4");
            majorVersions.put(8, "8.14.2");
            majorVersions.put(9, "9.2.0");

            GradleTestVersionsConfig config = GradleTestVersionsConfig.builder()
                    .majorVersions(majorVersions)
                    .extraVersions(new TreeSet<>())
                    .build();

            GradleTestVersionsConfig updated = config.withoutMajorVersion(8);

            assertThat(updated.majorVersions())
                    .containsEntry(7, "7.6.4")
                    .containsEntry(9, "9.2.0")
                    .doesNotContainKey(8);
        }

        @Test
        public void removes_matching_extra_versions() {
            SortedMap<Integer, String> majorVersions = new TreeMap<>();
            majorVersions.put(8, "8.14.2");
            majorVersions.put(9, "9.2.0");

            SortedSet<String> extraVersions = new TreeSet<>();
            extraVersions.add("8.8");
            extraVersions.add("8.10");
            extraVersions.add("9.0");

            GradleTestVersionsConfig config = GradleTestVersionsConfig.builder()
                    .majorVersions(majorVersions)
                    .extraVersions(extraVersions)
                    .build();

            GradleTestVersionsConfig updated = config.withoutMajorVersion(8);

            assertThat(updated.majorVersions()).doesNotContainKey(8);
            assertThat(updated.extraVersions()).containsExactly("9.0");
        }

        @Test
        public void keeps_unrelated_extra_versions() {
            SortedMap<Integer, String> majorVersions = new TreeMap<>();
            majorVersions.put(7, "7.6.4");
            majorVersions.put(8, "8.14.2");

            SortedSet<String> extraVersions = new TreeSet<>();
            extraVersions.add("7.5");
            extraVersions.add("8.8");

            GradleTestVersionsConfig config = GradleTestVersionsConfig.builder()
                    .majorVersions(majorVersions)
                    .extraVersions(extraVersions)
                    .build();

            GradleTestVersionsConfig updated = config.withoutMajorVersion(8);

            assertThat(updated.majorVersions()).containsEntry(7, "7.6.4");
            assertThat(updated.extraVersions()).containsExactly("7.5");
        }

        @Test
        public void handles_non_existent_major_versions() {
            SortedMap<Integer, String> majorVersions = new TreeMap<>();
            majorVersions.put(8, "8.14.2");

            GradleTestVersionsConfig config = GradleTestVersionsConfig.builder()
                    .majorVersions(majorVersions)
                    .extraVersions(new TreeSet<>())
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
            SortedMap<Integer, String> majorVersions = new TreeMap<>();
            majorVersions.put(8, "8.14.2");
            majorVersions.put(9, "9.2.0");

            SortedSet<String> extraVersions = new TreeSet<>();
            extraVersions.add("8.8");

            GradleTestVersionsConfig config = GradleTestVersionsConfig.builder()
                    .majorVersions(majorVersions)
                    .extraVersions(extraVersions)
                    .build();

            assertThat(config.majorVersions()).containsEntry(8, "8.14.2").containsEntry(9, "9.2.0");
            assertThat(config.extraVersions()).containsExactly("8.8");
        }

        @Test
        public void testEqualsAndHashCode() {
            SortedMap<Integer, String> majorVersions1 = new TreeMap<>();
            majorVersions1.put(8, "8.14.2");

            SortedSet<String> extraVersions1 = new TreeSet<>();
            extraVersions1.add("8.8");

            GradleTestVersionsConfig config1 = GradleTestVersionsConfig.builder()
                    .majorVersions(majorVersions1)
                    .extraVersions(extraVersions1)
                    .build();

            SortedMap<Integer, String> majorVersions2 = new TreeMap<>();
            majorVersions2.put(8, "8.14.2");

            SortedSet<String> extraVersions2 = new TreeSet<>();
            extraVersions2.add("8.8");

            GradleTestVersionsConfig config2 = GradleTestVersionsConfig.builder()
                    .majorVersions(majorVersions2)
                    .extraVersions(extraVersions2)
                    .build();

            assertThat(config1).isEqualTo(config2);
            assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
        }
    }
}
