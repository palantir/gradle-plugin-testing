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
import org.junit.jupiter.api.Test;

class GradleTestTemporaryFileTest {
    private final CompilationTestHelper compilationTestHelper =
            CompilationTestHelper.newInstance(GradleTestTemporaryFile.class, getClass());

    @Test
    void pick_up_manually_created_temporary_files_in_gradle_testing_class() {
        // language=Java
        compilationTestHelper.addSourceLines("TestClass.java", """
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import java.io.File;
            import java.nio.file.Files;
            import java.nio.file.Path;
            import org.apache.commons.io.FileUtils;

            @GradlePluginTests
            class TestClass {
                void test() throws Exception {
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
            }
            """).doTest();
    }

    @Test
    void allow_temporary_file_creation_outside_of_gradle_plugin_tests() {
        // language=Java
        compilationTestHelper.addSourceLines("TestClass.java", """
            import java.nio.file.Files;

            class TestClass {
                void test() throws Exception {
                    Files.createTempFile("prefix", "suffix");
                }
            }
            """).doTest();
    }

    @Test
    void allow_temporary_file_in_outer_class_if_nested_class_is_used_with_gradle_plugin_tests() {
        // language=Java
        compilationTestHelper.addSourceLines("TestClass.java", """
            import com.palantir.gradle.testing.junit.GradlePluginTests;
            import java.nio.file.Files;

            class TestClass {
                void test() throws Exception {
                    Files.createTempFile("prefix", "suffix");
                }

                @GradlePluginTests
                class Nested {
                    void nested_test() throws Exception {
                        // BUG: Diagnostic contains: GradleTestTemporaryFile
                        Files.createTempFile("prefix", "suffix");
                    }
                }
            }
            """).doTest();
    }
}
