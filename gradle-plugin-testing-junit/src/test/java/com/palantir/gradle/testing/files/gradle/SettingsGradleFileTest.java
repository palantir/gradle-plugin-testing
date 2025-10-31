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

            settingsGradleFile.assertThat().hasContent("something already here\nrootProject.name = 'name'\n");
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
                // before
                rootProject.name = 'name'
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

    @Test
    void all_sections_with_nested_pluginManagement_and_buildscript() {
        // Test all sections in reverse order to verify intelligent ordering
        settingsGradleFile.include("module1");
        settingsGradleFile.include("module2");
        settingsGradleFile.rootProjectName("my-app");
        settingsGradleFile.plugins().appendLine("id 'settings-plugin'");
        settingsGradleFile.buildscript().dependencies().appendLine("classpath 'com.example:plugin:1.0'");
        settingsGradleFile.buildscript().repositories().appendLine("mavenCentral()");
        settingsGradleFile.pluginManagement().plugins().appendLine("id 'plugin1' version '1.0'");
        settingsGradleFile.pluginManagement().repositories().appendLine("gradlePluginPortal()");

        // Verify full structure is correct
        settingsGradleFile.assertThat().hasContent("""
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                }
                plugins {
                    id 'plugin1' version '1.0'
                }
            }

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

            include 'module1'
            include 'module2'
            rootProject.name = 'my-app'
            """);
    }

    @Test
    void multiple_edits_to_same_section() {
        settingsGradleFile.pluginManagement().repositories().appendLine("gradlePluginPortal()");
        settingsGradleFile.plugins().appendLine("id 'first'");
        settingsGradleFile.pluginManagement().repositories().appendLine("mavenCentral()");
        settingsGradleFile.plugins().appendLine("id 'second'");
        settingsGradleFile.buildscript().repositories().appendLine("google()");

        settingsGradleFile.assertThat().hasContent("""
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    mavenCentral()
                }
            }

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
        settingsGradleFile.pluginManagement().repositories().appendLine("mavenCentral()");
        settingsGradleFile.pluginManagement().repositories().appendLine("google()");
        settingsGradleFile.pluginManagement().repositories().prependLine("gradlePluginPortal()");

        settingsGradleFile.plugins().appendLine("id 'original'");
        settingsGradleFile.plugins().overwrite("id 'replaced'");

        settingsGradleFile.buildscript().repositories().appendLine("mavenCentral()");
        settingsGradleFile.buildscript().repositories().edit(content -> content.replace("mavenCentral()", "google()"));

        settingsGradleFile.assertThat().hasContent("""
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    mavenCentral()
                    google()
                }
            }

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
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                }
            }

            plugins {
                id 'base'
            }

            rootProject.name = 'existing'
            """);

        settingsGradleFile.pluginManagement().repositories().appendLine("mavenCentral()");
        settingsGradleFile.buildscript().repositories().appendLine("google()");
        settingsGradleFile.include("new-module");

        settingsGradleFile.assertThat().hasContent("""
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    mavenCentral()
                }
            }

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
}
