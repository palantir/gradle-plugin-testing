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

package com.palantir.gradle.testing.files.gradle;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsGradleFileTest {
    SettingsGradleFile settingsGradleFile;

    @BeforeEach
    void beforeEach(@TempDir Path tempDir) {
        settingsGradleFile = new SettingsGradleFile(tempDir.resolve("settings.gradle"));
    }

    @Nested
    class RootProjectName {
        @Test
        void set_root_project_name() {
            settingsGradleFile.rootProjectName("blahblah");

            settingsGradleFile.assertThat().hasContent("rootProject.name = 'blahblah'\n");
        }

        @Test
        void change_root_project_name() {
            settingsGradleFile.rootProjectName("first");
            settingsGradleFile.rootProjectName("second");

            settingsGradleFile.assertThat().hasContent("rootProject.name = 'second'\n");
            settingsGradleFile.assertThat().content().doesNotContain("first");
        }

        @Test
        void maintains_ending_newlines() {
            settingsGradleFile.append("something already here\n\n");

            settingsGradleFile.rootProjectName("name");

            settingsGradleFile.assertThat().hasContent("rootProject.name = 'name'\n\nsomething already here\n");
        }

        @Test
        void replaces_insitu() {
            settingsGradleFile.overwrite("""
                // before

                rootProject.name = 'init'

                // after
                """);

            settingsGradleFile.rootProjectName("name");

            settingsGradleFile.assertThat().hasContent("""
                rootProject.name = 'name'

                // before
                // after
                """);
        }

        @Test
        void throws_when_multiple_rootProject_name_assignments_exist() {
            settingsGradleFile.overwrite("""
                // before
                rootProject.name = 'init'
                // during
                rootProject.name = 'second'
                // after
                """);

            assertThatIllegalStateException()
                    .isThrownBy(() -> settingsGradleFile.rootProjectName("name"))
                    .withMessageContaining("Found multiple rootProject.name assignments")
                    .withMessageContaining("use the rootProjectName() method instead");
        }
    }

    @Nested
    class Include {
        @Test
        void including_a_project_path_puts_it_in_the_settings_gradle() {
            settingsGradleFile.include("foo:bar:baz");

            settingsGradleFile.assertThat().content().contains("include 'foo:bar:baz'");
        }

        @Test
        void including_the_same_project_path_multiple_times_does_not_make_multiple_settings_entries() {
            settingsGradleFile.include("foo");
            settingsGradleFile.include("foo");

            settingsGradleFile.assertThat().content().containsOnlyOnce("foo");
        }
    }

    @Nested
    class StructuredSections {
        @Test
        void all_sections_with_nested_pluginManagement_and_buildscript() {
            // Test all sections in reverse order to verify intelligent ordering
            settingsGradleFile.include("module1");
            settingsGradleFile.include("module2");
            settingsGradleFile.rootProjectName("my-app");
            settingsGradleFile.plugins().appendLine("id 'settings-plugin'");
            settingsGradleFile.buildscript().dependencies().appendLine("classpath 'com.example:plugin:1.0'");
            settingsGradleFile.buildscript().repositories().appendLine("mavenCentral()");

            // Verify full structure is correct
            settingsGradleFile.assertThat().hasContent("""
                buildscript {
                    repositories {
                        mavenCentral()
                    }
                    dependencies {
                        classpath 'com.example:plugin:1.0'
                    }
                }

                plugins {
                    id 'settings-plugin'
                }

                rootProject.name = 'my-app'

                include 'module1'
                include 'module2'
                """);
        }

        @Test
        void multiple_edits_to_same_section() {
            settingsGradleFile.plugins().appendLine("id 'first'");
            settingsGradleFile.plugins().appendLine("id 'second'");
            settingsGradleFile.buildscript().repositories().appendLine("google()");

            settingsGradleFile.assertThat().hasContent("""
                buildscript {
                    repositories {
                        google()
                    }
                }

                plugins {
                    id 'first'
                    id 'second'
                }
                """);
        }

        @Test
        void section_operations_prepend_overwrite_edit() {
            settingsGradleFile.plugins().appendLine("id 'original'");
            settingsGradleFile.plugins().overwrite("id 'replaced'");

            settingsGradleFile.buildscript().repositories().appendLine("mavenCentral()");
            settingsGradleFile
                    .buildscript()
                    .repositories()
                    .edit(content -> content.replace("mavenCentral()", "google()"));

            settingsGradleFile.assertThat().hasContent("""
                buildscript {
                    repositories {
                        google()
                    }
                }

                plugins {
                    id 'replaced'
                }
                """);
        }

        @Test
        void include_deduplication() {
            settingsGradleFile.include("module-a");
            settingsGradleFile.include("module-b");
            settingsGradleFile.include("module-a");

            settingsGradleFile.assertThat().hasContent("""
                include 'module-a'
                include 'module-b'
                """);
        }

        @Test
        void rootProjectName_replacement() {
            settingsGradleFile.rootProjectName("first-name");
            settingsGradleFile.plugins().appendLine("id 'base'");
            settingsGradleFile.rootProjectName("second-name");

            settingsGradleFile.assertThat().hasContent("""
                plugins {
                    id 'base'
                }

                rootProject.name = 'second-name'
                """);
            settingsGradleFile.assertThat().content().doesNotContain("first-name");
        }

        @Test
        void editing_existing_file_with_structured_sections() {
            settingsGradleFile.append("""
                plugins {
                    id 'base'
                }

                rootProject.name = 'existing'
                """);

            settingsGradleFile.buildscript().repositories().appendLine("google()");
            settingsGradleFile.include("new-module");

            settingsGradleFile.assertThat().hasContent("""
                buildscript {
                    repositories {
                        google()
                    }
                }

                plugins {
                    id 'base'
                }

                rootProject.name = 'existing'

                include 'new-module'
                """);
        }

        @Test
        void block_editor_text_matches_expected_content() {
            settingsGradleFile.buildscript().repositories().appendLine("google()");
            settingsGradleFile.buildscript().dependencies().appendLine("classpath 'plugin:2.0'");
            settingsGradleFile.plugins().appendLine("id 'base'");
            settingsGradleFile.include("module-a");
            settingsGradleFile.include("module-b");

            settingsGradleFile.buildscript().assertThat().hasContent("""
                repositories {
                    google()
                }
                dependencies {
                    classpath 'plugin:2.0'
                }
                """);

            settingsGradleFile.buildscript().repositories().assertThat().hasContent("""
                google()
                """);

            settingsGradleFile.buildscript().dependencies().assertThat().hasContent("""
                classpath 'plugin:2.0'
                """);

            settingsGradleFile.plugins().assertThat().hasContent("""
                id 'base'
                """);
        }

        @Test
        void block_editor_text_empty_blocks() {
            settingsGradleFile.buildscript().repositories().assertThat().hasContent("");
            settingsGradleFile.plugins().assertThat().hasContent("");
        }
    }
}
