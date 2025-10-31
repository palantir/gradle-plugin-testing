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

            settingsGradleFile.assertThat().hasContent("rootProject.name = 'name'\nsomething already here\n\n");
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
    class PluginManagement {
        @Test
        void can_add_repository_to_plugin_management() {
            settingsGradleFile.pluginManagement().repositories().append("gradlePluginPortal()");

            settingsGradleFile.assertThat().content().contains("pluginManagement {");
            settingsGradleFile.assertThat().content().contains("repositories {");
            settingsGradleFile.assertThat().content().contains("gradlePluginPortal()");
        }

        @Test
        void can_add_plugins_to_plugin_management() {
            settingsGradleFile.pluginManagement().plugins().append("id 'com.example.plugin' version '1.0.0'");

            settingsGradleFile.assertThat().content().contains("pluginManagement {");
            settingsGradleFile.assertThat().content().contains("plugins {");
            settingsGradleFile.assertThat().content().contains("id 'com.example.plugin' version '1.0.0'");
        }

        @Test
        void plugin_management_appears_before_plugins_block() {
            settingsGradleFile.plugins().append("id 'com.example.base'");
            settingsGradleFile.pluginManagement().repositories().append("gradlePluginPortal()");

            String content = settingsGradleFile.text();
            int pluginManagementPos = content.indexOf("pluginManagement");
            int pluginsPos = content.indexOf("plugins {");

            org.assertj.core.api.Assertions.assertThat(pluginManagementPos).isLessThan(pluginsPos);
        }
    }

    @Nested
    class Plugins {
        @Test
        void can_add_plugins() {
            settingsGradleFile.plugins().append("id 'com.example.plugin' version '1.0.0'");

            settingsGradleFile.assertThat().content().contains("plugins {");
            settingsGradleFile.assertThat().content().contains("id 'com.example.plugin' version '1.0.0'");
        }
    }

    @Nested
    class BuildScript {
        @Test
        void can_add_repository_to_buildscript() {
            settingsGradleFile.buildscript().repositories().append("mavenCentral()");

            settingsGradleFile.assertThat().content().contains("buildscript {");
            settingsGradleFile.assertThat().content().contains("repositories {");
            settingsGradleFile.assertThat().content().contains("mavenCentral()");
        }

        @Test
        void can_add_dependencies_to_buildscript() {
            settingsGradleFile.buildscript().dependencies().append("classpath 'com.example:plugin:1.0.0'");

            settingsGradleFile.assertThat().content().contains("buildscript {");
            settingsGradleFile.assertThat().content().contains("dependencies {");
            settingsGradleFile.assertThat().content().contains("classpath 'com.example:plugin:1.0.0'");
        }

        @Test
        void buildscript_appears_between_pluginManagement_and_plugins() {
            settingsGradleFile.plugins().append("id 'com.example.base'");
            settingsGradleFile.pluginManagement().repositories().append("gradlePluginPortal()");
            settingsGradleFile.buildscript().repositories().append("mavenCentral()");

            String content = settingsGradleFile.text();
            int pluginManagementPos = content.indexOf("pluginManagement");
            int buildscriptPos = content.indexOf("buildscript");
            int pluginsPos = content.indexOf("plugins {");

            org.assertj.core.api.Assertions.assertThat(pluginManagementPos).isLessThan(buildscriptPos);
            org.assertj.core.api.Assertions.assertThat(buildscriptPos).isLessThan(pluginsPos);
        }
    }
}
