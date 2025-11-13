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

import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.Test;

@GradlePluginTests
class PluginsUsagesTest {

    @Test
    void can_add_plugin_to_empty_file(RootProject rootProject) {
        rootProject.buildGradle().plugins().add("java");

        rootProject.buildGradle().assertThat().hasContent("""
            plugins {
                id 'java'
            }
            """);
    }

    @Test
    void can_add_multiple_plugins(RootProject rootProject) {
        rootProject.buildGradle().plugins().add("java").add("application");

        rootProject.buildGradle().assertThat().hasContent("""
            plugins {
                id 'java'
                id 'application'
            }
            """);
    }

    @Test
    void adding_the_same_plugin_multiple_times_is_okay(RootProject rootProject) {
        rootProject.buildGradle().plugins().add("java").add("java").add("java");

        rootProject.buildGradle().assertThat().hasContent("""
            plugins {
                id 'java'
            }
            """);
    }

    @Test
    void multiple_add_calls_are_grouped_even_if_not_together(RootProject rootProject) {
        rootProject.buildGradle().plugins().add("java");
        rootProject.buildGradle().append("// comment");
        rootProject.buildGradle().plugins().add("application");

        rootProject.buildGradle().assertThat().hasContent("""
            plugins {
                id 'java'
                id 'application'
            }
            // comment
            """);
    }

    @Test
    void plugins_block_placed_after_buildscript(RootProject rootProject) {
        rootProject.buildGradle().overwrite("""
            buildscript {
                repositories {
                    mavenCentral()
                }
            }
            """);

        rootProject.buildGradle().plugins().add("java");

        rootProject.buildGradle().assertThat().hasContent("""
            buildscript {
                repositories {
                    mavenCentral()
                }
            }
            plugins {
                id 'java'
            }
            """);
    }

    @Test
    void plugins_work_in_settings_gradle(RootProject rootProject) {
        rootProject.settingsGradle().plugins().add("com.example.settings-plugin");

        rootProject.settingsGradle().assertThat().hasContent("""
            plugins {
                id 'com.example.settings-plugin'
            }
            rootProject.name = 'root'
            """);
    }

    @Test
    void can_add_plugin_with_apply_false(RootProject rootProject) {
        rootProject.buildGradle().plugins().addWithoutApply("com.example.plugin");

        rootProject.buildGradle().assertThat().hasContent("""
            plugins {
                id 'com.example.plugin' apply false
            }
            """);
    }

    @Test
    void can_prepend_after_adding_plugin(RootProject rootProject) {
        rootProject.buildGradle().plugins().add("java");
        rootProject.buildGradle().prepend("""
            // blah
            """);

        rootProject.buildGradle().assertThat().hasContent("""
            plugins {
                id 'java'
            }
            // blah
            """);
    }

    @Test
    void prepend_after_plugins_block_with_buildscript_keeps_correct_order(RootProject rootProject) {
        rootProject.buildGradle().plugins().add("java");
        rootProject.buildGradle().prepend("""
            buildscript {
                repositories {
                    mavenCentral()
                }
            }
            repositories {
                mavenCentral()
            }
            """);

        rootProject.buildGradle().assertThat().hasContent("""
            buildscript {
                repositories {
                    mavenCentral()
                }
            }
            plugins {
                id 'java'
            }
            repositories {
                mavenCentral()
            }
            """);
    }

    @Test
    void append_after_plugins_block_with_buildscript_keeps_correct_order(RootProject rootProject) {
        rootProject.buildGradle().plugins().add("java");
        rootProject.buildGradle().append("""
            buildscript {
                repositories {
                    mavenCentral()
                }
            }
            repositories {
                mavenCentral()
            }
            """);

        rootProject.buildGradle().assertThat().hasContent("""
            buildscript {
                repositories {
                    mavenCentral()
                }
            }
            plugins {
                id 'java'
            }
            repositories {
                mavenCentral()
            }
            """);
    }
}
