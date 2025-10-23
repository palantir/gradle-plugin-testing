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

package com.palantir.gradle.testing.files;

import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.Test;

@GradlePluginTests
class DirectoryTest {

    @Test
    void can_create_nested_directories(RootProject rootProject) {
        Directory dir = rootProject.directory("foo/bar/baz").ensureExists();

        assertThat(dir.path()).isDirectory();
    }

    @Test
    void can_create_files_in_directory(RootProject rootProject) {
        Directory dir = rootProject.directory("my-dir");

        dir.file("test.txt").overwrite("hello");

        assertThat(dir.file("test.txt").text()).isEqualTo("hello");
    }

    @Test
    void directory_implements_file_factory(RootProject rootProject) {
        Directory dir = rootProject.directory("my-dir");

        dir.file("file.txt").overwrite("file content");
        dir.gradleFile("build.gradle").append("plugins { id 'java' }");
        dir.yamlFile("config.yaml").overwrite("key: value");
        dir.propertiesFile("gradle.properties").overwrite("property=value");

        assertThat(dir.file("file.txt").text()).isEqualTo("file content");
        assertThat(dir.gradleFile("build.gradle").text()).contains("plugins { id 'java' }");
        assertThat(dir.yamlFile("config.yaml").text()).isEqualTo("key: value");
        assertThat(dir.propertiesFile("gradle.properties").text()).isEqualTo("property=value");
    }

    @Test
    void directory_not_created_by_default(RootProject rootProject) {
        Directory dir = rootProject.directory("my-dir");

        assertThat(dir.path()).doesNotExist();
    }
}
