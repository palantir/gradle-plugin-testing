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

import com.google.errorprone.CompilationTestHelper;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

class GradleTestStringFormattingTest {
    @Test
    void catch_formatting_in_project_file() {
        test("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.ProjectFile;

            @GradlePluginTests
            class TestClass {
                void test(ProjectFile file) {
                    // BUG: Diagnostic contains: GradleTestStringFormatting
                    file.append("foo %s".formatted(3));
                    // BUG: Diagnostic contains: GradleTestStringFormatting
                    file.appendLine("foo %s".formatted(3));
                    // BUG: Diagnostic contains: GradleTestStringFormatting
                    file.prepend("foo %s".formatted(3));
                    // BUG: Diagnostic contains: GradleTestStringFormatting
                    file.prependLine("foo %s".formatted(3));
                    // BUG: Diagnostic contains: GradleTestStringFormatting
                    file.overwrite("foo %s".formatted(3));
                    // BUG: Diagnostic contains: GradleTestStringFormatting
                    file.append(String.format("foo %s", 3));
                    // BUG: Diagnostic contains: GradleTestStringFormatting
                    file.appendLine(String.format("foo %s", 3));
                    // BUG: Diagnostic contains: GradleTestStringFormatting
                    file.prepend(String.format("foo %s", 3));
                    // BUG: Diagnostic contains: GradleTestStringFormatting
                    file.prependLine(String.format("foo %s", 3));
                    // BUG: Diagnostic contains: GradleTestStringFormatting
                    file.overwrite(String.format("foo %s", 3));
                }
            }
            """);
    }

    @Test
    void catch_formatting_in_java_src_dir_write_class() {
        test("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.java.JavaSrcDir;

            @GradlePluginTests
            class TestClass {
                void test(JavaSrcDir javaDir) {
                    // BUG: Diagnostic contains: GradleTestStringFormatting
                    javaDir.writeClass("class Foo { int bar = %d; }".formatted(42));
                    // BUG: Diagnostic contains: GradleTestStringFormatting
                    javaDir.writeClass(String.format("class Foo { int bar = %d; }", 42));
                }
            }
            """);
    }

    @Test
    void allow_direct_varargs_in_project_file() {
        test("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.ProjectFile;

            @GradlePluginTests
            class TestClass {
                void test(ProjectFile file) {
                    file.append("foo %s", 3);
                    file.overwrite("foo %s", 3);
                    file.appendLine("foo %s", 3);
                    file.prepend("foo %s", 3);
                    file.prependLine("foo %s", 3);
                }
            }
            """);
    }

    @Test
    void allow_direct_varargs_in_java_src_dir() {
        test("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.java.JavaSrcDir;

            @GradlePluginTests
            class TestClass {
                void test(JavaSrcDir javaDir) {
                    javaDir.writeClass("class Foo { int bar = %d; }", 42);
                }
            }
            """);
    }

    @Test
    void allow_formatted_strings_outside_of_gradle_plugin_tests() {
        test("""
            import com.palantir.gradle.testing.files.ProjectFile;

            class TestClass {
                void test(ProjectFile file) {
                    file.append("foo %s".formatted(3));
                    file.append(String.format("foo %s", 3));
                }
            }
            """);
    }

    @Test
    void allow_formatted_strings_on_non_project_file_methods() {
        test("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;

            @GradlePluginTests
            class TestClass {
                void test() {
                    System.out.println("foo %s".formatted(3));
                    System.out.println(String.format("foo %s", 3));
                }
            }
            """);
    }

    @Test
    void catch_pre_formatted_variable_with_formatted() {
        test("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.ProjectFile;

            @GradlePluginTests
            class TestClass {
                void test(ProjectFile file) {
                    String str = "hi %s".formatted("world");
                    // BUG: Diagnostic contains: GradleTestStringFormatting
                    file.append(str);
                }
            }
            """);
    }

    @Test
    void catch_pre_formatted_variable_with_string_format() {
        test("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.ProjectFile;

            @GradlePluginTests
            class TestClass {
                void test(ProjectFile file) {
                    String str = String.format("hi %s", "world");
                    // BUG: Diagnostic contains: GradleTestStringFormatting
                    file.append(str);
                }
            }
            """);
    }

    @Test
    void catch_pre_formatted_variable_in_java_src_dir() {
        test("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.java.JavaSrcDir;

            @GradlePluginTests
            class TestClass {
                void test(JavaSrcDir javaDir) {
                    String javaCode = "class Foo { int bar = %d; }".formatted(42);
                    // BUG: Diagnostic contains: GradleTestStringFormatting
                    javaDir.writeClass(javaCode);
                }
            }
            """);
    }

    @Test
    void allow_pre_formatted_variable_for_other_uses() {
        test("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;

            @GradlePluginTests
            class TestClass {
                void test() {
                    String str = "hi %s".formatted("world");
                    System.out.println(str);
                }
            }
            """);
    }

    @Test
    void allow_non_formatted_variable() {
        test("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.ProjectFile;

            @GradlePluginTests
            class TestClass {
                void test(ProjectFile file) {
                    String str = "hello";
                    file.append(str);
                }
            }
            """);
    }

    private void test(@Language("Java") String javaCode) {
        CompilationTestHelper compilationTestHelper =
                CompilationTestHelper.newInstance(GradleTestStringFormatting.class, getClass());
        compilationTestHelper.addSourceLines("TestClass.java", javaCode).doTest();
    }
}
