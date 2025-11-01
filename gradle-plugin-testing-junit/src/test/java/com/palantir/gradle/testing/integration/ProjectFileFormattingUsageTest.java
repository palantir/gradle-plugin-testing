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

package com.palantir.gradle.testing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.Test;

@GradlePluginTests
class ProjectFileFormattingUsageTest {

    @Test
    void can_format_overwrite(RootProject rootProject) {
        rootProject.buildGradle().overwrite("""
            tasks.register('%s') {
                doLast {}
            }
            """, "myTask");

        assertThat(rootProject.buildGradle().text()).contains("tasks.register('myTask')");
    }

    @Test
    void can_format_override_manually(RootProject rootProject) {
        rootProject.buildGradle().overwrite("""
            tasks.register('%s') {
                doLast {}
            }
            """.formatted("myTask"));

        assertThat(rootProject.buildGradle().text()).contains("tasks.register('myTask')");
    }

    @Test
    void can_format_append(RootProject rootProject) {
        rootProject.buildGradle().plugins().append("id 'java'");
        rootProject.buildGradle().append("""
            tasks.register('%s') {
                doLast {}
            }
            """, "myTask");

        assertThat(rootProject.buildGradle().text()).contains("""
            plugins {
                id 'java'
            }
            """).contains("tasks.register('myTask')");
    }

    @Test
    void can_format_append_manually(RootProject rootProject) {
        rootProject.buildGradle().plugins().append("id 'java'");
        rootProject.buildGradle().append("""
            tasks.register('%s') {
                doLast {}
            }
            """.formatted("myTask"));

        assertThat(rootProject.buildGradle().text()).contains("""
            plugins {
                id 'java'
            }
            """).contains("tasks.register('myTask')");
    }

    @Test
    void can_format_appendLine(RootProject rootProject) {
        rootProject.buildGradle().appendLine("version = '%s'", "1.0.0");

        assertThat(rootProject.buildGradle().text()).isEqualTo("version = '1.0.0'\n");
    }

    @Test
    void can_format_appendLine_manually(RootProject rootProject) {
        rootProject.buildGradle().appendLine("version = '%s'".formatted("1.0.0"));

        assertThat(rootProject.buildGradle().text()).isEqualTo("version = '1.0.0'\n");
    }

    @Test
    void can_format_prepend(RootProject rootProject) {
        rootProject.buildGradle().overwrite("task foo {}\n");
        rootProject.buildGradle().prepend("""
            tasks.register('%s') {
                doLast {}
            }
            """, "myTask");

        assertThat(rootProject.buildGradle().text()).startsWith("tasks.register('myTask')");
    }

    @Test
    void can_format_prepend_manually(RootProject rootProject) {
        rootProject.buildGradle().overwrite("task foo {}\n");
        rootProject.buildGradle().prepend("""
            tasks.register('%s') {
                doLast {}
            }
            """.formatted("myTask"));

        assertThat(rootProject.buildGradle().text()).startsWith("tasks.register('myTask')");
    }

    @Test
    void can_format_prependLine(RootProject rootProject) {
        rootProject.buildGradle().overwrite("task foo {}");
        rootProject.buildGradle().prependLine("version = '%s'", "1.0.0");

        assertThat(rootProject.buildGradle().text()).startsWith("version = '1.0.0'\n");
    }

    @Test
    void can_format_prependLine_manually(RootProject rootProject) {
        rootProject.buildGradle().overwrite("task foo {}");
        rootProject.buildGradle().prependLine("version = '%s'".formatted("1.0.0"));

        assertThat(rootProject.buildGradle().text()).startsWith("version = '1.0.0'\n");
    }

    @Test
    void can_format_with_multiple_arguments(RootProject rootProject) {
        rootProject.buildGradle().overwrite("""
            tasks.register('%s') {
                group = '%s'
                description = '%s'
            }
            """, "myTask", "build", "My custom task");

        assertThat(rootProject.buildGradle().text())
                .contains("tasks.register('myTask')")
                .contains("group = 'build'")
                .contains("description = 'My custom task'");
    }

    @Test
    void works_without_format_arguments(RootProject rootProject) {
        rootProject.buildGradle().overwrite("""
            tasks.register('myTask') {
                doLast {}
            }
            """);

        assertThat(rootProject.buildGradle().text()).contains("tasks.register('myTask')");
    }

    @Test
    void formatting_preserves_language_injection_in_ide(RootProject rootProject) {
        // This test documents that by using varargs instead of .formatted(),
        // IDE language injections are preserved in the text block
        String taskName = "customTask";

        rootProject.buildGradle().append("""
            tasks.register('%s') {
                doLast {
                    println 'Hello from task'
                }
            }
            """, taskName);

        assertThat(rootProject.buildGradle().text()).contains("tasks.register('customTask')");
    }
}
