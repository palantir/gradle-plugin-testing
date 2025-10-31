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

import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@GradlePluginTests
class BuildGradleFileTest {

    @Nested
    class Repositories {
        @Test
        void can_add_repository_to_empty_file(RootProject rootProject) {
            rootProject.buildGradle().repositories().append("mavenCentral()");

            assertThat(rootProject.buildGradle().text()).isEqualTo("""
                repositories {
                    mavenCentral()
                }\
                """);
        }

        @Test
        void can_add_multiple_repositories(RootProject rootProject) {
            rootProject.buildGradle().repositories().append("mavenCentral()");
            rootProject.buildGradle().repositories().append("google()");

            assertThat(rootProject.buildGradle().text()).contains("mavenCentral()");
            assertThat(rootProject.buildGradle().text()).contains("google()");
        }
    }

    @Nested
    class Buildscript {
        @Test
        void buildscript_appears_before_plugins_block(RootProject rootProject) {
            rootProject.buildGradle().plugins().append("id 'java'");
            rootProject.buildGradle().buildscript().repositories().append("mavenCentral()");

            String content = rootProject.buildGradle().text();
            int buildscriptPos = content.indexOf("buildscript");
            int pluginsPos = content.indexOf("plugins");

            assertThat(buildscriptPos).isLessThan(pluginsPos);
        }

        @Test
        void buildscript_repositories_and_dependencies(RootProject rootProject) {
            rootProject.buildGradle().buildscript().repositories().append("mavenCentral()");
            rootProject.buildGradle().buildscript().dependencies().append("classpath 'com.example:plugin:1.0.0'");

            String content = rootProject.buildGradle().text();
            assertThat(content).contains("buildscript {");
            assertThat(content).contains("repositories {");
            assertThat(content).contains("mavenCentral()");
            assertThat(content).contains("dependencies {");
            assertThat(content).contains("classpath 'com.example:plugin:1.0.0'");
        }
    }

    @Nested
    class BackwardsCompatibility {
        @Test
        void can_still_use_direct_append(RootProject rootProject) {
            rootProject.buildGradle().append("""
                version = '1.0.0'

                task myTask {
                    println 'Hello'
                }
                """);

            assertThat(rootProject.buildGradle().text()).contains("version = '1.0.0'");
            assertThat(rootProject.buildGradle().text()).contains("task myTask");
        }

        @Test
        void structured_sections_work_with_direct_append(RootProject rootProject, GradleInvoker gradle) {
            rootProject.buildGradle().plugins().append("id 'java'");
            rootProject.buildGradle().repositories().append("mavenCentral()");
            rootProject.buildGradle().append("\nversion = '1.0.0'\n");

            assertThat(gradle.withArgs("build").buildsSuccessfully()).isNotNull();
        }
    }

    @Nested
    class IntelligentOrdering {
        @Test
        void maintains_proper_gradle_ordering_with_multiple_sections(RootProject rootProject) {
            // Add in reverse order to test intelligent placement
            rootProject.buildGradle().dependencies().append("implementation 'com.google.guava:guava:32.0.0-jre'");
            rootProject.buildGradle().repositories().append("mavenCentral()");
            rootProject.buildGradle().plugins().append("id 'java'");
            rootProject.buildGradle().buildscript().repositories().append("mavenCentral()");

            String content = rootProject.buildGradle().text();
            int buildscriptPos = content.indexOf("buildscript");
            int pluginsPos = content.indexOf("plugins");
            int repositoriesPos = content.indexOf("repositories {", buildscriptPos + 50); // Skip buildscript repos
            int dependenciesPos = content.indexOf("dependencies");

            assertThat(buildscriptPos).isLessThan(pluginsPos);
            assertThat(pluginsPos).isLessThan(repositoriesPos);
            assertThat(repositoriesPos).isLessThan(dependenciesPos);
        }
    }

    @Nested
    class ProjectScopes {
        @Test
        void allprojects_and_subprojects_sections(RootProject rootProject) {
            rootProject.buildGradle().allprojects().append("group = 'com.example'");
            rootProject.buildGradle().subprojects().append("apply plugin: 'java'");

            String content = rootProject.buildGradle().text();
            assertThat(content).contains("allprojects {");
            assertThat(content).contains("group = 'com.example'");
            assertThat(content).contains("subprojects {");
            assertThat(content).contains("apply plugin: 'java'");
        }
    }
}
