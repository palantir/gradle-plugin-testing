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
        testUnchanged("""
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
        testUnchanged("""
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
        testUnchanged("""
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

    @Test
    void allow_formatted_strings_when_no_format_method_overload_exists() {
        test("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.java.JavaSrcDir;

            @GradlePluginTests
            class TestClass {
                void test(JavaSrcDir javaDir) {
                    javaDir.fileByClassName("com.example.%s".formatted("MyClass"));
                    javaDir.fileByPath("com/example/%s.java".formatted("MyClass"));
                    javaDir.fileByClassName(String.format("com.example.%s", "MyClass"));
                    javaDir.fileByPath(String.format("com/example/%s.java", "MyClass"));
                }
            }
            """);
    }

    @Test
    void autofix_formatted_strings_in_project_file() {
        testFix("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.ProjectFile;

            @GradlePluginTests
            class TestClass {
                void test(ProjectFile file) {
                    file.append("foo %s".formatted(3));
                    file.appendLine("foo %s bar %d".formatted("hello", 42));
                    file.prepend(String.format("foo %s bar %d", "hello", 42));
                    file.prependLine("test %s".formatted("value"));
                    file.overwrite("content %s".formatted("data"));
                }
            }
            """, """
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import com.palantir.gradle.testing.files.ProjectFile;

            @GradlePluginTests
            class TestClass {
                void test(ProjectFile file) {
                    file.append("foo %s", 3);
                    file.appendLine("foo %s bar %d", "hello", 42);
                    file.prepend("foo %s bar %d", "hello", 42);
                    file.prependLine("test %s", "value");
                    file.overwrite("content %s", "data");
                }
            }
            """);
    }

    private void test(@Language("Java") String javaCode) {
        CompilationTestHelper compilationTestHelper =
                CompilationTestHelper.newInstance(GradleTestStringFormatting.class, getClass());
        compilationTestHelper.addSourceLines("TestClass.java", javaCode).doTest();
    }

    private void testFix(@Language("Java") String input, @Language("Java") String expected) {
        BugCheckerRefactoringTestHelper refactoringTestHelper =
                BugCheckerRefactoringTestHelper.newInstance(GradleTestStringFormatting.class, getClass());
        refactoringTestHelper
                .addInputLines("TestClass.java", input)
                .addOutputLines("TestClass.java", expected)
                .doTest();
    }

    private void testUnchanged(@Language("Java") String javaCode) {
        BugCheckerRefactoringTestHelper refactoringTestHelper =
                BugCheckerRefactoringTestHelper.newInstance(GradleTestStringFormatting.class, getClass());
        refactoringTestHelper
                .addInputLines("TestClass.java", javaCode)
                .expectUnchanged()
                .doTest();
    }
}
