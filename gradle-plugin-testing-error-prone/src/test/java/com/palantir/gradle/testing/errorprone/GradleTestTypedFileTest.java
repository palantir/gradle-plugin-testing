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

package com.palantir.gradle.testing.errorprone;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

class GradleTestTypedFileTest {
    @Test
    void catch_file_calls_with_typed_alternatives() {
        test("""
            import com.palantir.gradle.testing.files.Directory;
            import com.palantir.gradle.testing.junit.GradlePluginTests;

            @GradlePluginTests
            class TestClass {
                void test(Directory dir) {
                    // BUG: Diagnostic contains: GradleTestTypedFile
                    dir.file("config.yml");
                    // BUG: Diagnostic contains: GradleTestTypedFile
                    dir.file("config.yaml");
                    // BUG: Diagnostic contains: GradleTestTypedFile
                    dir.file("build.gradle");
                    // BUG: Diagnostic contains: GradleTestTypedFile
                    dir.file("gradle.properties");
                }
            }
            """);
    }

    @Test
    void catch_chained_directory_file() {
        test("""
            import com.palantir.gradle.testing.files.Directory;
            import com.palantir.gradle.testing.junit.GradlePluginTests;

            @GradlePluginTests
            class TestClass {
                void test(Directory dir) {
                    // BUG: Diagnostic contains: GradleTestTypedFile
                    dir.directory("foo").file("bar.yml");
                }
            }
            """);
    }

    @Test
    void catch_instances_inside_nested_classes() {
        test("""
            import com.palantir.gradle.testing.files.Directory;
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import org.junit.jupiter.api.Nested;

            @GradlePluginTests
            class TestClass {
                @Nested
                class NestedClass {
                    void test(Directory dir) {
                        // BUG: Diagnostic contains: GradleTestTypedFile
                        dir.file("config.yml");
                    }
                }
            }
            """);
    }

    @Test
    void allow_file_calls_without_typed_alternatives() {
        test("""
            import com.palantir.gradle.testing.files.Directory;
            import com.palantir.gradle.testing.junit.GradlePluginTests;

            @GradlePluginTests
            class TestClass {
                void test(Directory dir) {
                    dir.file("readme.txt");
                    dir.file("data.json");
                    dir.file("foo.gradle.kts");
                    dir.file("config.xml");
                    dir.file("notes.md");
                }
            }
            """);
    }

    @Test
    void allow_file_calls_with_variable_arguments() {
        test("""
            import com.palantir.gradle.testing.files.Directory;
            import com.palantir.gradle.testing.junit.GradlePluginTests;

            @GradlePluginTests
            class TestClass {
                void test(Directory dir) {
                    String filename = "config.yml";
                    dir.file(filename);
                }
            }
            """);
    }

    @Test
    void allow_file_calls_outside_gradle_plugin_tests() {
        test("""
            import com.palantir.gradle.testing.files.Directory;

            class TestClass {
                void test(Directory dir) {
                    dir.file("config.yml");
                }
            }
            """);
    }

    @Test
    void autofix_yml_extension() {
        testFix("""
            import com.palantir.gradle.testing.files.Directory;
            import com.palantir.gradle.testing.junit.GradlePluginTests;

            @GradlePluginTests
            class TestClass {
                void test(Directory dir) {
                    dir.file("config.yml");
                }
            }
            """, """
            import com.palantir.gradle.testing.files.Directory;
            import com.palantir.gradle.testing.junit.GradlePluginTests;

            @GradlePluginTests
            class TestClass {
                void test(Directory dir) {
                    dir.yamlFile("config.yml");
                }
            }
            """);
    }

    @Test
    void autofix_yaml_extension() {
        testFix("""
            import com.palantir.gradle.testing.files.Directory;
            import com.palantir.gradle.testing.junit.GradlePluginTests;

            @GradlePluginTests
            class TestClass {
                void test(Directory dir) {
                    dir.file("config.yaml");
                }
            }
            """, """
            import com.palantir.gradle.testing.files.Directory;
            import com.palantir.gradle.testing.junit.GradlePluginTests;

            @GradlePluginTests
            class TestClass {
                void test(Directory dir) {
                    dir.yamlFile("config.yaml");
                }
            }
            """);
    }

    @Test
    void autofix_gradle_extension() {
        testFix("""
            import com.palantir.gradle.testing.files.Directory;
            import com.palantir.gradle.testing.junit.GradlePluginTests;

            @GradlePluginTests
            class TestClass {
                void test(Directory dir) {
                    dir.file("build.gradle");
                }
            }
            """, """
            import com.palantir.gradle.testing.files.Directory;
            import com.palantir.gradle.testing.junit.GradlePluginTests;

            @GradlePluginTests
            class TestClass {
                void test(Directory dir) {
                    dir.gradleFile("build.gradle");
                }
            }
            """);
    }

    @Test
    void autofix_properties_extension() {
        testFix("""
            import com.palantir.gradle.testing.files.Directory;
            import com.palantir.gradle.testing.junit.GradlePluginTests;

            @GradlePluginTests
            class TestClass {
                void test(Directory dir) {
                    dir.file("gradle.properties");
                }
            }
            """, """
            import com.palantir.gradle.testing.files.Directory;
            import com.palantir.gradle.testing.junit.GradlePluginTests;

            @GradlePluginTests
            class TestClass {
                void test(Directory dir) {
                    dir.propertiesFile("gradle.properties");
                }
            }
            """);
    }

    @Test
    void autofix_chained_directory_file() {
        testFix("""
            import com.palantir.gradle.testing.files.Directory;
            import com.palantir.gradle.testing.junit.GradlePluginTests;

            @GradlePluginTests
            class TestClass {
                void test(Directory dir) {
                    dir.directory("foo").file("bar.yml");
                }
            }
            """, """
            import com.palantir.gradle.testing.files.Directory;
            import com.palantir.gradle.testing.junit.GradlePluginTests;

            @GradlePluginTests
            class TestClass {
                void test(Directory dir) {
                    dir.directory("foo").yamlFile("bar.yml");
                }
            }
            """);
    }

    private void test(@Language("Java") String javaCode) {
        CompilationTestHelper.newInstance(GradleTestTypedFile.class, getClass())
                .addSourceLines("TestClass.java", javaCode)
                .doTest();
    }

    private void testFix(@Language("Java") String input, @Language("Java") String expected) {
        BugCheckerRefactoringTestHelper.newInstance(GradleTestTypedFile.class, getClass())
                .addInputLines("TestClass.java", input)
                .addOutputLines("TestClass.java", expected)
                .doTest();
    }
}
