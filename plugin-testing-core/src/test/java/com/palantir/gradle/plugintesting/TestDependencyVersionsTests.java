/*
 * (c) Copyright 2024 Palantir Technologies Inc. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TestDependencyVersionsTests {
    private static final String SAMPLE_VERSIONS = "foo:bar=100\ncom.palantir:gradle-plugin-testing=1.2.3";

    @TempDir
    static Path tempDir;

    @BeforeAll
    public static void beforeAll() {
        Path versionsFile = tempDir.resolve("dependency-versions.properties");
        System.setProperty(
                TestDependencyVersions.TEST_DEPENDENCIES_FILE_SYSTEM_PROPERTY,
                versionsFile.toAbsolutePath().toString());
        try {
            Files.writeString(versionsFile, SAMPLE_VERSIONS);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testDepVersions() {
        assertThat(TestDependencyVersions.resolve("foo:bar")).isEqualTo("foo:bar:100");
        assertThat(TestDependencyVersions.resolve("com.palantir:gradle-plugin-testing"))
                .isEqualTo("com.palantir:gradle-plugin-testing:1.2.3");
    }

    @Test
    public void throwWhenNoVersion() {
        assertThatThrownBy(() -> TestDependencyVersions.resolve("not:found"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No version found for not:found");
    }
}
