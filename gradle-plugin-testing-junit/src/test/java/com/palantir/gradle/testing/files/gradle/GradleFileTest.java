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

import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.Test;

@GradlePluginTests
class GradleFileTest {
    @Test
    void addDependencies_single_dependency_with_default_configuration(RootProject rootProject) {
        rootProject.buildGradle().addDependencies("com.google.guava:guava:31.1-jre");

        assertThat(rootProject.buildGradle().text()).contains("""
            dependencies {
                implementation 'com.google.guava:guava:31.1-jre'
            }
            """);
    }

    @Test
    void addDependencies_multiple_dependencies_with_default_configuration(RootProject rootProject) {
        rootProject.buildGradle().addDependencies("com.google.guava:guava:31.1-jre", "org.slf4j:slf4j-api:2.0.0");

        assertThat(rootProject.buildGradle().text()).contains("""
            dependencies {
                implementation 'com.google.guava:guava:31.1-jre'
                implementation 'org.slf4j:slf4j-api:2.0.0'
            }
            """);
    }

    @Test
    void addDependency_with_custom_configuration(RootProject rootProject) {
        rootProject.buildGradle().addDependency("testImplementation", "junit:junit:4.13.2");

        assertThat(rootProject.buildGradle().text()).contains("""
            dependencies {
                testImplementation 'junit:junit:4.13.2'
            }
            """);
    }

    @Test
    void addDependencies_can_be_chained(RootProject rootProject) {
        rootProject
                .buildGradle()
                .addDependencies("com.google.guava:guava:31.1-jre")
                .addDependency("testImplementation", "junit:junit:4.13.2");

        assertThat(rootProject.buildGradle().text()).contains("""
            dependencies {
                implementation 'com.google.guava:guava:31.1-jre'
            }
            dependencies {
                testImplementation 'junit:junit:4.13.2'
            }
            """);
    }

    @Test
    void addDependency_with_multiple_dependencies_using_varargs(RootProject rootProject) {
        rootProject
                .buildGradle()
                .addDependency("testImplementation", "junit:junit:4.13.2", "org.mockito:mockito-core:4.8.0");

        assertThat(rootProject.buildGradle().text()).contains("""
            dependencies {
                testImplementation 'junit:junit:4.13.2'
                testImplementation 'org.mockito:mockito-core:4.8.0'
            }
            """);
    }
}
