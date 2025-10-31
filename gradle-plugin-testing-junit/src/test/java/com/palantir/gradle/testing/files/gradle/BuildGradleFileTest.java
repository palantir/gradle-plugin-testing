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
        rootProject.buildGradle().subprojects().append("apply plugin: 'java'");
        rootProject.buildGradle().allprojects().append("group = 'com.example'");
        rootProject.buildGradle().dependencies().append("implementation 'com.google.guava:guava:32.0.0-jre'");
        rootProject.buildGradle().repositories().append("mavenCentral()");
        rootProject.buildGradle().plugins().append("id 'java'");
        rootProject.buildGradle().buildscript().dependencies().append("classpath 'plugin:1.0'");
        rootProject.buildGradle().buildscript().repositories().append("gradlePluginPortal()");

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
        rootProject.buildGradle().plugins().append("id 'java'");
        rootProject.buildGradle().repositories().append("mavenCentral()");
        rootProject.buildGradle().repositories().append("google()");
        rootProject.buildGradle().dependencies().append("implementation 'junit:junit:4.13.2'");
        rootProject.buildGradle().plugins().append("id 'maven-publish'");

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
        rootProject.buildGradle().repositories().append("mavenCentral()");
        rootProject.buildGradle().repositories().append("google()");
        rootProject.buildGradle().repositories().prepend("gradlePluginPortal()");

        rootProject.buildGradle().dependencies().append("implementation 'a:b:1'");
        rootProject.buildGradle().dependencies().overwrite("testImplementation 'x:y:2'");

        rootProject.buildGradle().plugins().append("id 'original'");
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
        rootProject.buildGradle().buildscript().repositories().append("mavenCentral()");
        rootProject.buildGradle().plugins().append("id 'java'");
        rootProject.buildGradle().buildscript().repositories().append("google()");
        rootProject.buildGradle().buildscript().dependencies().append("classpath 'first:1.0'");
        rootProject.buildGradle().buildscript().dependencies().append("classpath 'second:2.0'");

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
        rootProject.buildGradle().plugins().append("id 'java'");
        rootProject.buildGradle().append("version = '1.0.0'");
        rootProject.buildGradle().repositories().append("mavenCentral()");

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

        rootProject.buildGradle().repositories().append("mavenCentral()");
        rootProject.buildGradle().dependencies().append("implementation 'junit:junit:4.13.2'");
        rootProject.buildGradle().buildscript().repositories().append("gradlePluginPortal()");

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
        rootProject.buildGradle().append("""
            // Custom configuration
            version = '1.0.0'
            group = 'com.example'
            """);

        rootProject.buildGradle().plugins().append("id 'java'");

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
        rootProject.buildGradle().plugins().append("""
            // Application plugins
            id 'java'
            // Publishing
            id 'maven-publish'
            """);

        rootProject.buildGradle().repositories().append("mavenCentral()");

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

        rootProject.buildGradle().dependencies().append("implementation 'junit:junit:4.13.2'");

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

        rootProject.buildGradle().repositories().append("mavenCentral()");

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
        rootProject.buildGradle().plugins().append("id 'java'");

        rootProject.buildGradle().append("""
            tasks.named('test') {
                useJUnitPlatform()
            }
            """);

        rootProject.buildGradle().repositories().append("mavenCentral()");

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
}
