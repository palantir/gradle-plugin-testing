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

class GradleTestTemporaryFileTest {
    @Test
    void pick_up_manually_created_temporary_files_in_gradle_testing_class() {
        test("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import java.io.File;
            import java.nio.file.Files;
            import java.nio.file.Path;
            import org.apache.commons.io.FileUtils;
            import org.junit.jupiter.api.io.TempDir;

            @GradlePluginTests
            class TestClass {
                // BUG: Diagnostic contains: GradleTestTemporaryFile
                @TempDir Path tempDirField;

                // BUG: Diagnostic contains: GradleTestTemporaryFile
                void test(@TempDir Path tempDir) throws Exception {
                    // BUG: Diagnostic contains: GradleTestTemporaryFile
                    Files.createTempDirectory("prefix");
                    // BUG: Diagnostic contains: GradleTestTemporaryFile
                    Files.createTempDirectory(Path.of("."), "prefix");
                    // BUG: Diagnostic contains: GradleTestTemporaryFile
                    Files.createTempFile("prefix", "suffix");
                    // BUG: Diagnostic contains: GradleTestTemporaryFile
                    Files.createTempFile(Path.of("."), "prefix", "suffix");

                    // BUG: Diagnostic contains: GradleTestTemporaryFile
                    File.createTempFile("prefix", "suffix");
                    // BUG: Diagnostic contains: GradleTestTemporaryFile
                    File.createTempFile("prefix", "suffix", new File("."));

                    // BUG: Diagnostic contains: GradleTestTemporaryFile
                    com.google.common.io.Files.createTempDir();

                    // BUG: Diagnostic contains: GradleTestTemporaryFile
                    FileUtils.getTempDirectory();
                    // BUG: Diagnostic contains: GradleTestTemporaryFile
                    FileUtils.getTempDirectoryPath();
                }

                // BUG: Diagnostic contains: GradleTestTemporaryFile
                void someMethod(@TempDir Path tempDir) {}
            }
            """);
    }

    @Test
    void allow_temporary_file_creation_outside_of_gradle_plugin_tests() {
        test("""
            import java.nio.file.Files;
            import java.nio.file.Path;
            import org.junit.jupiter.api.io.TempDir;

            class TestClass {
                @TempDir Path tempDirField;

                void test(@TempDir Path tempDir) throws Exception {
                    Files.createTempFile("prefix", "suffix");
                }
            }
            """);
    }

    @Test
    void allow_temporary_file_in_outer_class_if_nested_class_is_used_with_gradle_plugin_tests() {
        test("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import java.nio.file.Path;
            import java.nio.file.Files;
            import org.junit.jupiter.api.Nested;
            import org.junit.jupiter.api.io.TempDir;

            class TestClass {
                @TempDir Path tempDirField;

                void test(@TempDir Path tempDir) throws Exception {
                    Files.createTempFile("prefix", "suffix");
                }

                @Nested
                @GradlePluginTests
                class NestedClass {
                    // BUG: Diagnostic contains: GradleTestTemporaryFile
                    @TempDir Path nestedTempDirField;

                    // BUG: Diagnostic contains: GradleTestTemporaryFile
                    void nested_test(@TempDir Path tempDir) throws Exception {
                        // BUG: Diagnostic contains: GradleTestTemporaryFile
                        Files.createTempFile("prefix", "suffix");
                    }
                }
            }
            """);
    }

    @Test
    void catch_instances_inside_nested_classes() {
        test("""
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import java.nio.file.Path;
            import java.nio.file.Files;
            import org.junit.jupiter.api.Nested;
            import org.junit.jupiter.api.io.TempDir;

            @GradlePluginTests
            class TestClass {
                @Nested
                class NestedClass {
                    // BUG: Diagnostic contains: GradleTestTemporaryFile
                    @TempDir Path nestedTempDirField;

                    // BUG: Diagnostic contains: GradleTestTemporaryFile
                    void nested_test(@TempDir Path tempDir) throws Exception {
                        // BUG: Diagnostic contains: GradleTestTemporaryFile
                        Files.createTempFile("prefix", "suffix");
                    }
                }
            }
            """);
    }

    private void test(@Language("Java") String javaCode) {
        CompilationTestHelper compilationTestHelper =
                CompilationTestHelper.newInstance(GradleTestTemporaryFile.class, getClass());
        compilationTestHelper.addSourceLines("TestClass.java", javaCode).doTest();
    }
}
