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

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Splitter;
import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.execution.InvocationResult;
import com.palantir.gradle.testing.files.Directory;
import com.palantir.gradle.testing.junit.DisabledConfigurationCache;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.junit.WithJdkAutomanagement;
import com.palantir.gradle.testing.project.RootProject;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@GradlePluginTests
class PluginTestingJunitPluginTest {

    @BeforeEach
    void beforeEach(RootProject rootProject) {
        rootProject
                .gradlePropertiesFile()
                .appendProperty(PluginTestingPlugin.PLUGIN_VERSION_PROPERTY_NAME, System.getProperty("projectVersion"));

        rootProject
                .buildGradle()
                .plugins()
                .add("groovy")
                .add("com.palantir.gradle-plugin-testing")
                .add("java-gradle-plugin")
                .add("com.palantir.consistent-versions");

        rootProject.buildGradle().append("""
            repositories {
                mavenCentral()
                mavenLocal()
            }

            dependencies {
                testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.13.4'
                testRuntimeOnly 'org.junit.platform:junit-platform-runner:1.14.0'

                testImplementation 'com.netflix.nebula:nebula-test'
            }

            tasks.withType(Test).configureEach {
                useJUnitPlatform()
            }

            gradleTestUtils {
                gradleVersions = ['7.6.5', '8.14.3']
            }
            """);

        rootProject.file("versions.lock").createEmpty();
    }

    @Test
    void can_add_external_gradle_plugins_for_testing(GradleInvoker gradleInvoker, RootProject rootProject) {

        rootProject.buildGradle().append("""
            dependencies {
                gradlePluginForTesting 'com.palantir.sls-packaging:gradle-sls-packaging'
            }
            """);

        rootProject.propertiesFile("versions.props").appendProperty("com.palantir.sls-packaging:*", "7.84.0");

        rootProject.testSourceSet().java().writeClass("""
            package test;

            import com.palantir.gradle.testing.execution.GradleInvoker;
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.project.RootProject;
            import org.junit.jupiter.api.Test;

            @GradlePluginTests
            class TestClass {
                @Test
                void testMethod(GradleInvoker gradle, RootProject rootProject) {

                    rootProject.buildGradle().plugins().add("com.palantir.sls-asset-distribution");
                    rootProject.buildGradle().append(\"""
                        def pluginVersion = plugins.getPlugin('com.palantir.sls-asset-distribution').getClass().package.implementationVersion
                        println "plugin version: $pluginVersion"
                        ""\");

                    gradle.withArgs().buildsSuccessfully().assertThat().output().contains("plugin version: 7.84.0");
                }
            }
            """);

        gradleInvoker.withArgs("test").buildsSuccessfully();
    }

    @Test
    void confirm_gradlePluginForTesting_is_locked(GradleInvoker gradleInvoker, RootProject rootProject) {

        rootProject.buildGradle().append("""
            dependencies {
                gradlePluginForTesting 'com.palantir.sls-packaging:gradle-sls-packaging'
            }
            """);

        rootProject.propertiesFile("versions.props").appendProperty("com.palantir.sls-packaging:*", "7.84.0");

        gradleInvoker.withArgs("writeVersionsLock").buildsSuccessfully();
        rootProject
                .file("versions.lock")
                .assertThat()
                .content()
                .containsSubsequence("[Test dependencies]", "com.palantir.sls-packaging:gradle-sls-packaging:7.84.0");
    }

    @Test
    void correct_versions_from_extension_are_used_by_junit_library(
            GradleInvoker gradleInvoker, RootProject rootProject) {

        rootProject.testSourceSet().java().writeClass("""
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
                .file("gradle-plugin-testing/TestClass/testMethod/7.6.5/gradle-version")
                .assertThat()
                .hasContent("7.6.5");

        rootProject
                .buildDir()
                .file("gradle-plugin-testing/TestClass/testMethod/8.14.3/gradle-version")
                .assertThat()
                .hasContent("8.14.3");
    }

    @Test
    void errorprones_are_injected_automatically(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().plugins().add("net.ltgt.errorprone");

        rootProject.buildGradle().append("""
            dependencies {
                errorprone 'com.google.errorprone:error_prone_core:2.42.0'
            }

            afterEvaluate {
                tasks.withType(JavaCompile).configureEach {
                    options.errorprone {
                        // By default, suppressible-error-prone includes the build directory in the excluded paths,
                        // meaning our test error-prone never runs
                        // TODO(callumr): setup error-prone authoring plugin for suppressible-error-prone that disables this
                        excludedPaths = ''
                    }
                }
            }
            """);

        rootProject.testSourceSet().java().writeClass("""
            package test;

            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import java.nio.file.Files;
            import org.junit.jupiter.api.Test;

            @GradlePluginTests
            class TestClass {
                @Test
                void test() throws Exception {
                    Files.createTempDirectory("prefix");
                }
            }
            """);

        assertThat(gradle.withArgs("test").buildsWithFailure().output()).contains("[GradleTestTemporaryFile]");
    }

    @Test
    void gradlePluginTests_are_discovered(GradleInvoker gradle, RootProject rootProject) throws IOException {
        rootProject.testSourceSet().java().writeClass("""
            package test;

            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import java.nio.file.Files;
            import org.junit.jupiter.api.Nested;
            import org.junit.jupiter.api.Test;

            @GradlePluginTests
            class GradlePluginTestClass {
                @Test
                void test() throws Exception {
                    Files.createTempDirectory("prefix");
                }

                @Nested
                class NestedGradlePluginTestClass {

                    @Test
                    void nested_test() throws Exception {
                        Files.createTempDirectory("other");
                    }
                }
            }
            """);

        rootProject.testSourceSet().java().writeClass("""
            package test;

            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.junit.DisabledConfigurationCache;
            import java.nio.file.Files;
            import org.junit.jupiter.api.Nested;
            import org.junit.jupiter.api.Test;

            @GradlePluginTests
            @DisabledConfigurationCache
            class NoConfigCacheGradlePluginTestClass {
                @Test
                void test() throws Exception {
                    Files.createTempDirectory("prefix");
                }
            }
            """);

        rootProject.testSourceSet().java().writeClass("""
            package test;

            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import org.junit.jupiter.api.Test;

            class JunitTest {
                @Test
                void my_junit_test() {
                }
            }
            """);

        gradle.withArgs("discoverGradlePluginTestsWithDisabledConfigurationCache")
                .buildsSuccessfully();
        assertThat(readTestClassesPaths(rootProject, "java"))
                .containsExactlyInAnyOrder("src/test/java/test/NoConfigCacheGradlePluginTestClass.java");
    }

    @Test
    void nebula_tests_ready_for_migration_are_discovered(GradleInvoker gradle, RootProject rootProject)
            throws IOException {
        Directory groovyDir =
                rootProject.testSourceSet().srcDir("groovy").directory("test").createDirectories();

        Map<String, String> testNameToClass = Map.of(
                // included because it extends IntegrationSpec
                "NebulaIntegrationTest",
                "nebula.test.IntegrationSpec",
                // included because it extends IntegrationSpec
                "SubClassesNebulaIntegrationTest",
                "test.NebulaIntegrationTest",
                // included because it extends IntegrationSpec
                "NebulaIntegrationTestKitSpec",
                "nebula.test.IntegrationTestKitSpec",
                // ignored because it extends ConfigurationCacheSpec
                "IgnoredConfigurationCacheTest",
                "com.palantir.gradle.plugintesting.ConfigurationCacheSpec",
                // ignored because it doesn't extend an allowlisted class
                "IgnoredSpecification",
                "spock.lang.Specification");
        testNameToClass.forEach((testName, importClass) -> {
            List<String> parts = Splitter.on('.').splitToList(importClass);

            String classToExtend = parts.get(parts.size() - 1);
            groovyDir.file(String.format("%s.groovy", testName)).overwrite("""
                package test;

                import %s
                import java.nio.file.Files;

                class %s extends %s {

                    def '#param: my test'() {
                        Files.createTempDirectory("prefix" + gradleVersion);

                        where:
                        param << [1, 2]
                    }
                }
                """, importClass, testName, classToExtend);
        });

        gradle.withArgs("discoverNebulaTestClassesToMigrate").buildsSuccessfully();
        assertThat(readTestClassesPaths(rootProject, "groovy"))
                .containsExactlyInAnyOrder(
                        "src/test/groovy/test/NebulaIntegrationTest.groovy",
                        "src/test/groovy/test/SubClassesNebulaIntegrationTest.groovy",
                        "src/test/groovy/test/NebulaIntegrationTestKitSpec.groovy");
    }

    @Test
    @WithJdkAutomanagement
    @DisabledConfigurationCache("gradle-jdks are not yet compatible with CC")
    void javaToolchains_are_correctly_set_when_jdkAutomanagement_is_enabled(
            GradleInvoker invoker, RootProject rootProject) {
        rootProject.buildGradle().plugins().add("com.palantir.jdks.latest");
        rootProject.buildGradle().append("""
            jdks {
                daemonTarget = 21
            }
            """);
        InvocationResult result = invoker.withArgs("javaToolchains").buildsSuccessfully();
        result.assertThat().output().contains("Auto-detection:     Disabled");
        result.assertThat().output().contains("Auto-download:      Disabled");
    }

    private static List<String> readTestClassesPaths(RootProject rootProject, String language) throws IOException {
        return Files.readAllLines(rootProject
                .buildDir()
                .directory(String.format("tests-discovery/%s", language))
                .file("test-classes-paths")
                .path());
    }
}
