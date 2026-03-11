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

class GradleTestAppendPropertyTest {

    @Test
    void catch_appendProperty_in_gradle_plugin_tests() {
        test("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.properties.PropertiesFile;

            @GradlePluginTests
            class TestClass {
                void test(PropertiesFile props) {
                    // BUG: Diagnostic contains: GradleTestAppendProperty
                    props.appendProperty("key", "value");
                }
            }
            """);
    }

    @Test
    void allow_appendProperty_outside_gradle_plugin_tests() {
        test("""
            import com.palantir.gradle.testing.files.properties.PropertiesFile;

            class TestClass {
                void test(PropertiesFile props) {
                    props.appendProperty("key", "value");
                }
            }
            """);
    }

    @Test
    void allow_setProperty_in_gradle_plugin_tests() {
        test("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.properties.PropertiesFile;

            @GradlePluginTests
            class TestClass {
                void test(PropertiesFile props) {
                    props.setProperty("key", "value");
                }
            }
            """);
    }

    @Test
    void autofix_appendProperty_to_setProperty() {
        testFix("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.properties.PropertiesFile;

            @GradlePluginTests
            class TestClass {
                void test(PropertiesFile props) {
                    props.appendProperty("key", "value");
                }
            }
            """, """
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.properties.PropertiesFile;

            @GradlePluginTests
            class TestClass {
                void test(PropertiesFile props) {
                    props.setProperty("key", "value");
                }
            }
            """);
    }

    @Test
    void autofix_chained_appendProperty_calls() {
        testFix("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.properties.PropertiesFile;

            @GradlePluginTests
            class TestClass {
                void test(PropertiesFile props) {
                    props.appendProperty("key", "val").appendProperty("k2", "v2");
                }
            }
            """, """
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.properties.PropertiesFile;

            @GradlePluginTests
            class TestClass {
                void test(PropertiesFile props) {
                    props.setProperty("key", "val").setProperty("k2", "v2");
                }
            }
            """);
    }

    @Test
    void autofix_appendProperty_on_separate_line_after_setProperty() {
        testFix("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.properties.PropertiesFile;

            @GradlePluginTests
            class TestClass {
                void test(PropertiesFile props) {
                    props.setProperty("org.slf4j:*", "1.7.21");

                    props
                        .setProperty("key1", "val1")
                        .appendProperty("key2", "val2");
                }
            }
            """, """
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.properties.PropertiesFile;

            @GradlePluginTests
            class TestClass {
                void test(PropertiesFile props) {
                    props.setProperty("org.slf4j:*", "1.7.21");

                    props
                        .setProperty("key1", "val1")
                        .setProperty("key2", "val2");
                }
            }
            """);
    }

    @Test
    void autofix_appendProperty_chained_on_new_line_from_different_statement() {
        testFix("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.properties.PropertiesFile;

            @GradlePluginTests
            class TestClass {
                void test(PropertiesFile props) {
                    props.setProperty("org.slf4j:*", "1.7.21");

                    props
                            .appendProperty("ch.qos.logback:logback-classic", "1.1.11");
                }
            }
            """, """
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.properties.PropertiesFile;

            @GradlePluginTests
            class TestClass {
                void test(PropertiesFile props) {
                    props.setProperty("org.slf4j:*", "1.7.21");

                    props
                            .setProperty("ch.qos.logback:logback-classic", "1.1.11");
                }
            }
            """);
    }

    private void test(@Language("Java") String javaCode) {
        CompilationTestHelper.newInstance(GradleTestAppendProperty.class, getClass())
                .addSourceLines("TestClass.java", javaCode)
                .doTest();
    }

    private void testFix(@Language("Java") String input, @Language("Java") String expected) {
        BugCheckerRefactoringTestHelper.newInstance(GradleTestAppendProperty.class, getClass())
                .addInputLines("TestClass.java", input)
                .addOutputLines("TestClass.java", expected)
                .doTest();
    }
}
