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

package com.palantir.gradle.plugintesting;

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@GradlePluginTests
class PluginTestingJunitPluginTest {
    @BeforeEach
    void beforeEach(RootProject rootProject) {
        rootProject
                .gradlePropertiesFile()
                .appendProperty(PluginTestingPlugin.PLUGIN_VERSION_PROPERTY_NAME, System.getProperty("projectVersion"));
    }

    @Test
    void correct_versions_from_extension_are_used_by_junit_library(
            GradleInvoker gradleInvoker, RootProject rootProject) {

        rootProject
                .buildGradle()
                .append(
                        """
            plugins {
                id 'com.palantir.gradle-plugin-testing'
                id 'java-gradle-plugin'
            }

            repositories {
                mavenCentral()
                mavenLocal()
            }

            dependencies {
                testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.13.4'
                testRuntimeOnly 'org.junit.platform:junit-platform-runner:1.14.0'
            }

            tasks.withType(Test).configureEach {
                useJUnitPlatform()
            }

            gradleTestUtils {
                gradleVersions = ['7.6.5', '8.14.3']
            }
            """);

        rootProject
                .testSourceSet()
                .java()
                .writeClass(
                        """
            package test;

            import com.palantir.gradle.testing.execution.GradleInvoker;
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.project.RootProject;
            import org.junit.jupiter.api.Test;

            @GradlePluginTests
            class TestClass {
                @Test
                void testMethod(GradleInvoker gradle, RootProject rootProject) {
                    rootProject.buildGradle().append(\"""
                        import org.gradle.util.GradleVersion
                        file('gradle-version') << GradleVersion.current().version
                        \""");

                    gradle.withArgs().buildsSuccessfully();
                }
            }
            """);

        gradleInvoker.withArgs("test").buildsSuccessfully();

        rootProject
                .buildDir()
                .file("gradle-testing/TestClass/testMethod/7.6.5/gradle-version")
                .assertThat()
                .hasContent("7.6.5");

        rootProject
                .buildDir()
                .file("gradle-testing/TestClass/testMethod/8.14.3/gradle-version")
                .assertThat()
                .hasContent("8.14.3");
    }
}
