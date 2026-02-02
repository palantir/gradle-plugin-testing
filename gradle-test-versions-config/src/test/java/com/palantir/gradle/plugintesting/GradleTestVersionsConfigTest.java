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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("for-rollout:FinalClass")
public class GradleTestVersionsConfigTest {
    private GradleTestVersionsConfigTest() {}

    public static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    @Nested
    class Deserialize {
        @Test
        public void deserialize_ok() throws IOException {
            GradleTestVersionsConfig config = deserialize("""
                major-versions:
                  8: 8.14.2
                  9: 9.2.0
                extra-versions:
                  - 8.8
                """);

            GradleTestVersionsConfig expected = GradleTestVersionsConfig.builder()
                    .putMajorVersions(8, "8.14.2")
                    .putMajorVersions(9, "9.2.0")
                    .addExtraVersions("8.8")
                    .build();

            assertThat(config).isEqualTo(expected);
        }

        @Test
        public void deserialize_without_extra_versions_ok() throws IOException {
            GradleTestVersionsConfig config = deserialize("""
                major-versions:
                  8: 8.14.2
                  9: 9.2.0
                """);

            GradleTestVersionsConfig expected = GradleTestVersionsConfig.builder()
                    .putMajorVersions(8, "8.14.2")
                    .putMajorVersions(9, "9.2.0")
                    .build();

            assertThat(config).isEqualTo(expected);
        }

        @Test
        public void deserialize_with_rc_ok() throws IOException {
            GradleTestVersionsConfig config = deserialize("""
                major-versions:
                  8: 8.14.2
                  9: 9.2.0-rc1
                extra-versions:
                  - 8.8
                """);

            GradleTestVersionsConfig expected = GradleTestVersionsConfig.builder()
                    .putMajorVersions(8, "8.14.2")
                    .putMajorVersions(9, "9.2.0-rc1")
                    .addExtraVersions("8.8")
                    .build();

            assertThat(config).isEqualTo(expected);
        }

        @Test
        public void deserialize_extra_versions_only_ok() throws IOException {
            GradleTestVersionsConfig config = deserialize("""
                extra-versions:
                  - 8.8
                  - 8.9
                  - 9.0
                """);

            GradleTestVersionsConfig expected = GradleTestVersionsConfig.builder()
                    .addExtraVersions("8.8")
                    .addExtraVersions("8.9")
                    .addExtraVersions("9.0")
                    .build();

            assertThat(config).isEqualTo(expected);
        }

        private static GradleTestVersionsConfig deserialize(String content) throws IOException {
            return YAML_MAPPER.readValue(content, new TypeReference<>() {});
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

            GradleTestVersionsConfig expected = GradleTestVersionsConfig.builder()
                    .putMajorVersions(7, "7.6.4")
                    .putMajorVersions(9, "9.2.0")
                    .build();

            assertThat(updated).isEqualTo(expected);
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

            GradleTestVersionsConfig expected = GradleTestVersionsConfig.builder()
                    .putMajorVersions(9, "9.2.0")
                    .addExtraVersions("9.0")
                    .build();

            assertThat(updated).isEqualTo(expected);
        }

        @Test
        public void handles_non_existent_major_versions() {
            GradleTestVersionsConfig config = GradleTestVersionsConfig.builder()
                    .putMajorVersions(8, "8.14.2")
                    .build();

            GradleTestVersionsConfig updated = config.withoutMajorVersion(9);

            GradleTestVersionsConfig expected = GradleTestVersionsConfig.builder()
                    .putMajorVersions(8, "8.14.2")
                    .build();

            assertThat(updated).isEqualTo(expected);
        }
    }
}
