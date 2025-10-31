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

import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.Test;

@GradlePluginTests
class BuildGradleFileTest {

    @Test
    void all_sections_with_nested_buildscript(RootProject rootProject) {
        // Test all section methods in reverse order to verify intelligent ordering
        rootProject.buildGradle().subprojects().appendLine("apply plugin: 'java'");
        rootProject.buildGradle().allprojects().appendLine("group = 'com.example'");
        rootProject.buildGradle().dependencies().appendLine("implementation 'com.google.guava:guava:32.0.0-jre'");
        rootProject.buildGradle().repositories().appendLine("mavenCentral()");
        rootProject.buildGradle().plugins().appendLine("id 'java'");
        rootProject.buildGradle().buildscript().dependencies().appendLine("classpath 'plugin:1.0'");
        rootProject.buildGradle().buildscript().repositories().appendLine("gradlePluginPortal()");

        // Verify full structure is correct
        rootProject.buildGradle().assertThat().hasContent("""
            buildscript {
                repositories {
                    gradlePluginPortal()
                }
                dependencies {
                    classpath 'plugin:1.0'
                }
            }

            plugins {
                id 'java'
            }

            allprojects {
                group = 'com.example'
            }

            subprojects {
                apply plugin: 'java'
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                implementation 'com.google.guava:guava:32.0.0-jre'
            }
            """);
    }

    @Test
    void multiple_edits_to_same_section(RootProject rootProject) {
        rootProject.buildGradle().plugins().appendLine("id 'java'");
        rootProject.buildGradle().repositories().appendLine("mavenCentral()");
        rootProject.buildGradle().repositories().appendLine("google()");
        rootProject.buildGradle().dependencies().appendLine("implementation 'junit:junit:4.13.2'");
        rootProject.buildGradle().plugins().appendLine("id 'maven-publish'");

        rootProject.buildGradle().assertThat().hasContent("""
            plugins {
                id 'java'
                id 'maven-publish'
            }

            repositories {
                mavenCentral()
                google()
            }

            dependencies {
                implementation 'junit:junit:4.13.2'
            }
            """);
    }

    @Test
    void section_operations_prepend_overwrite_edit(RootProject rootProject) {
        rootProject.buildGradle().repositories().appendLine("mavenCentral()");
        rootProject.buildGradle().repositories().appendLine("google()");
        rootProject.buildGradle().repositories().prependLine("gradlePluginPortal()");

        rootProject.buildGradle().dependencies().appendLine("implementation 'a:b:1'");
        rootProject.buildGradle().dependencies().overwrite("testImplementation 'x:y:2'");

        rootProject.buildGradle().plugins().appendLine("id 'original'");
        rootProject.buildGradle().plugins().edit(content -> content.replace("original", "modified"));

        rootProject.buildGradle().assertThat().hasContent("""
            plugins {
                id 'modified'
            }

            repositories {
                gradlePluginPortal()
                mavenCentral()
                google()
            }

            dependencies {
                testImplementation 'x:y:2'
            }
            """);
    }

    @Test
    void nested_buildscript_with_multiple_edits(RootProject rootProject) {
        rootProject.buildGradle().buildscript().repositories().appendLine("mavenCentral()");
        rootProject.buildGradle().plugins().appendLine("id 'java'");
        rootProject.buildGradle().buildscript().repositories().appendLine("google()");
        rootProject.buildGradle().buildscript().dependencies().appendLine("classpath 'first:1.0'");
        rootProject.buildGradle().buildscript().dependencies().appendLine("classpath 'second:2.0'");

        rootProject.buildGradle().assertThat().hasContent("""
            buildscript {
                repositories {
                    mavenCentral()
                    google()
                }
                dependencies {
                    classpath 'first:1.0'
                    classpath 'second:2.0'
                }
            }

            plugins {
                id 'java'
            }
            """);
    }

    @Test
    void direct_append_with_structured_sections(RootProject rootProject) {
        rootProject.buildGradle().plugins().appendLine("id 'java'");
        rootProject.buildGradle().appendLine("version = '1.0.0'");
        rootProject.buildGradle().repositories().appendLine("mavenCentral()");

        rootProject.buildGradle().assertThat().hasContent("""
            plugins {
                id 'java'
            }

            repositories {
                mavenCentral()
            }

            version = '1.0.0'
            """);
    }

    @Test
    void editing_existing_file_with_structured_sections(RootProject rootProject) {
        rootProject.buildGradle().append("""
            plugins {
                id 'base'
            }
            repositories {
                google()
            }
            version = '1.0.0'
            """);

        rootProject.buildGradle().repositories().appendLine("mavenCentral()");
        rootProject.buildGradle().dependencies().appendLine("implementation 'junit:junit:4.13.2'");
        rootProject.buildGradle().buildscript().repositories().appendLine("gradlePluginPortal()");

        rootProject.buildGradle().assertThat().hasContent("""
            buildscript {
                repositories {
                    gradlePluginPortal()
                }
            }

            plugins {
                id 'base'
            }

            repositories {
                google()
                mavenCentral()
            }

            dependencies {
                implementation 'junit:junit:4.13.2'
            }

            version = '1.0.0'
            """);
    }

    @Test
    void unrecognized_content_preserved(RootProject rootProject) {
        rootProject.buildGradle().appendLine("""
            // Custom configuration
            version = '1.0.0'
            group = 'com.example'
            """);

        rootProject.buildGradle().plugins().appendLine("id 'java'");

        rootProject.buildGradle().assertThat().hasContent("""
            plugins {
                id 'java'
            }

            // Custom configuration
            version = '1.0.0'
            group = 'com.example'
            """);
    }

    @Test
    void comments_within_sections_preserved(RootProject rootProject) {
        rootProject.buildGradle().plugins().appendLine("""
            // Application plugins
            id 'java'
            // Publishing
            id 'maven-publish'
            """);

        rootProject.buildGradle().repositories().appendLine("mavenCentral()");

        rootProject.buildGradle().assertThat().hasContent("""
            plugins {
                // Application plugins
                id 'java'
                // Publishing
                id 'maven-publish'
            }

            repositories {
                mavenCentral()
            }
            """);
    }

    @Test
    void comments_before_sections_become_unrecognized(RootProject rootProject) {
        // Comments before section blocks are extracted as unrecognized content
        // and moved to the end after all sections
        rootProject.buildGradle().append("""
            // Plugin configuration
            plugins {
                id 'java'
            }

            // Repository configuration
            repositories {
                mavenCentral()
            }
            """);

        rootProject.buildGradle().dependencies().appendLine("implementation 'junit:junit:4.13.2'");

        rootProject.buildGradle().assertThat().hasContent("""
            plugins {
                id 'java'
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                implementation 'junit:junit:4.13.2'
            }

            // Plugin configuration
            // Repository configuration
            """);
    }

    @Test
    void mixed_unrecognized_content_and_sections(RootProject rootProject) {
        rootProject.buildGradle().append("""
            version = '1.0.0'

            plugins {
                id 'base'
            }

            description = 'My project'
            """);

        rootProject.buildGradle().repositories().appendLine("mavenCentral()");

        rootProject.buildGradle().assertThat().hasContent("""
            plugins {
                id 'base'
            }

            repositories {
                mavenCentral()
            }

            version = '1.0.0'
            description = 'My project'
            """);
    }

    @Test
    void tasks_block_preserved_as_unrecognized(RootProject rootProject) {
        rootProject.buildGradle().plugins().appendLine("id 'java'");

        rootProject.buildGradle().append("""
            tasks.named('test') {
                useJUnitPlatform()
            }
            """);

        rootProject.buildGradle().repositories().appendLine("mavenCentral()");

        rootProject.buildGradle().assertThat().hasContent("""
            plugins {
                id 'java'
            }

            repositories {
                mavenCentral()
            }

            tasks.named('test') {
                useJUnitPlatform()
            }
            """);
    }

    @Test
    void two_level_nesting_configurations_all(RootProject rootProject) {
        // Test 2-level nesting: configurations { all { ... } }
        rootProject.buildGradle().plugins().appendLine("id 'java'");
        rootProject
                .buildGradle()
                .configurations()
                .all()
                .appendLine("exclude group: 'commons-logging', module: 'commons-logging'");
        rootProject.buildGradle().repositories().appendLine("mavenCentral()");

        rootProject.buildGradle().assertThat().hasContent("""
            plugins {
                id 'java'
            }

            repositories {
                mavenCentral()
            }

            configurations {
                all {
                    exclude group: 'commons-logging', module: 'commons-logging'
                }
            }
            """);
    }

    @Test
    void three_level_nesting_configurations_all_resolutionStrategy(RootProject rootProject) {
        // Test 3-level nesting: configurations { all { resolutionStrategy { ... } } }
        rootProject.buildGradle().plugins().appendLine("id 'java'");
        rootProject
                .buildGradle()
                .configurations()
                .all()
                .resolutionStrategy()
                .appendLine("force 'com.google.guava:guava:32.0.0-jre'");
        rootProject.buildGradle().repositories().appendLine("mavenCentral()");

        rootProject.buildGradle().assertThat().hasContent("""
            plugins {
                id 'java'
            }

            repositories {
                mavenCentral()
            }

            configurations {
                all {
                    resolutionStrategy {
                        force 'com.google.guava:guava:32.0.0-jre'
                    }
                }
            }
            """);
    }
}
