/*
 * (c) Copyright 2024 Palantir Technologies Inc. All rights reserved.
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
class TestDependencyVersionsTaskTest {

    @BeforeEach
    void beforeEach(RootProject rootProject) {
        rootProject
                .gradlePropertiesFile()
                .appendProperty(PluginTestingPlugin.PLUGIN_VERSION_PROPERTY_NAME, System.getProperty("projectVersion"));
    }

    @Test
    void write_versions_without_GCV(GradleInvoker gradleInvoker, RootProject rootProject) {
        rootProject.buildGradle().plugins().add("groovy");
        rootProject.buildGradle().plugins().add("com.palantir.gradle-plugin-testing");

        // language=gradle
        rootProject.buildGradle().append("""
            repositories {
                mavenCentral()
                mavenLocal()
            }

            dependencies {
                //WARNING: Do not include any dependencies here that the plugin-testing-core project uses in it's
                // api or implementation configurations.  Because the plugin adds the plugin-testing-core artifact as
                // a testImplementation dependency, we get the transitive dependencies of it (e.g. guava).  Since those
                // versions will update over time this test could start erroneously failing.  So just list some random
                // dependencies here that are unlikely to be added to the plugin-test-core project.
                //implementation 'com.google.guava:guava:33.3.1-jre'
                implementation 'org.ow2.asm:asm:9.7.1'

                testImplementation 'org.apache.httpcomponents.client5:httpclient5:5.3.1'

                testRuntimeOnly 'com.palantir.gradle.consistentversions:gradle-consistent-versions:2.31.0'
            }
            """);

        gradleInvoker.withArgs("writeTestDependencyVersions").buildsSuccessfully();

        rootProject
                .buildDir()
                .file("plugin-testing/dependency-versions.properties")
                .assertThat()
                .exists();

        rootProject
                .buildDir()
                .file("plugin-testing/dependency-versions.properties")
                .assertThat()
                .content()
                .doesNotContain("null")
                .contains("org.ow2.asm:asm=9.7.1")
                .contains("org.apache.httpcomponents.client5:httpclient5=5.3.1")
                .contains("com.palantir.gradle.consistentversions:gradle-consistent-versions=2.31.0");

        for (String name : PluginTestingPlugin.CORE_MAVEN_NAMES) {
            rootProject
                    .buildDir()
                    .file("plugin-testing/dependency-versions.properties")
                    .assertThat()
                    .content()
                    .contains("com.palantir.gradle.plugintesting:" + name);
        }
    }

    @Test
    void write_versions_with_GCV(GradleInvoker gradleInvoker, RootProject rootProject) {
        // remember - the resolved version for this dependency is using the information passed from the version of the
        // plugin
        // applied to the gradle-plugin-test project itself, _not_ the current version under test.  So the
        // addBuildScriptDependencies code and the "resolve" logic it calls is the current version, but the information
        // it is working with is from the last published version of the plugin (assuming that's the one applied to the
        // root build.gradle file of this project.
        rootProject
                .buildGradle()
                .append(TestContentHelpers.addBuildScriptBlock(
                        "mavenCentral()", "com.palantir.gradle.consistentversions:gradle-consistent-versions"));

        rootProject.buildGradle().plugins().add("com.palantir.consistent-versions");
        rootProject.buildGradle().plugins().add("groovy");
        rootProject.buildGradle().plugins().add("com.palantir.gradle-plugin-testing");

        // language=gradle
        rootProject.buildGradle().append("""
            repositories {
                mavenCentral()
                mavenLocal()
            }

            dependencies {
                implementation 'com.google.guava:guava'

                testImplementation 'org.junit.jupiter:junit-jupiter'
                testImplementation 'com.netflix.nebula:nebula-test'

                testRuntimeOnly 'com.palantir.gradle.consistentversions:gradle-consistent-versions'
            }
            """);

        rootProject
                .propertiesFile("versions.props")
                .appendProperty(
                        "org.junit.jupiter:junit-jupiter",
                        TestDependencyVersions.version("org.junit.jupiter:junit-jupiter"))
                .appendProperty(
                        "com.netflix.nebula:nebula-test",
                        TestDependencyVersions.version("com.netflix.nebula:nebula-test"))
                .appendProperty("com.google.guava:guava", TestDependencyVersions.version("com.google.guava:guava"))
                .appendProperty(
                        "com.palantir.gradle.consistentversions:gradle-consistent-versions",
                        TestDependencyVersions.version(
                                "com.palantir.gradle.consistentversions:gradle-consistent-versions"));

        gradleInvoker.withArgs("writeVersionLocks").buildsSuccessfully();

        gradleInvoker.withArgs("writeTestDependencyVersions").buildsSuccessfully();

        rootProject
                .buildDir()
                .file("plugin-testing/dependency-versions.properties")
                .assertThat()
                .exists();

        rootProject
                .buildDir()
                .file("plugin-testing/dependency-versions.properties")
                .assertThat()
                .content()
                .doesNotContain("null")
                .contains("org.junit.jupiter:junit-jupiter");

        for (String name : PluginTestingPlugin.CORE_MAVEN_NAMES) {
            rootProject
                    .buildDir()
                    .file("plugin-testing/dependency-versions.properties")
                    .assertThat()
                    .content()
                    .contains("com.palantir.gradle.plugintesting:" + name);
        }
    }
}
