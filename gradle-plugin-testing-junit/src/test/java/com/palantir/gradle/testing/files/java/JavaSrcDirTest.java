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

package com.palantir.gradle.testing.files.java;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JavaSrcDirTest {
    @TempDir
    Path srcDirPath;

    JavaSrcDir javaSrcDir;

    @BeforeEach
    void beforeEach() {
        javaSrcDir = new JavaSrcDir(srcDirPath);
    }

    @Nested
    class WriteClass {
        @Test
        void writes_class_out_to_correct_location() {
            String javaSource = """
                package foo;
                class SomeClass {}
                """;

            javaSrcDir.writeClass(javaSource);

            assertThat(srcDirPath.resolve("foo/SomeClass.java")).hasContent(javaSource);
        }

        @Test
        void writes_interface_out_to_correct_location() {
            String javaSource =
                    """
                package foo;
                interface SomeInterface {}
                """;

            javaSrcDir.writeClass(javaSource);

            assertThat(srcDirPath.resolve("foo/SomeInterface.java")).hasContent(javaSource);
        }

        @Test
        void writes_record_out_to_correct_location() {
            String javaSource =
                    """
                package foo;
                record SomeRecord(int hi) {}
                """;

            javaSrcDir.writeClass(javaSource);

            assertThat(srcDirPath.resolve("foo/SomeRecord.java")).hasContent(javaSource);
        }

        @Test
        void writes_enum_out_to_correct_location() {
            String javaSource =
                    """
                package foo;
                enum SomeRecord(int hi) {}
                """;

            javaSrcDir.writeClass(javaSource);

            assertThat(srcDirPath.resolve("foo/SomeRecord.java")).hasContent(javaSource);
        }

        @Test
        void writes_annotation_interface_out_to_correct_location() {
            String javaSource =
                    """
                package foo;
                @interface SomeAnnotation {}
                """;

            javaSrcDir.writeClass(javaSource);

            assertThat(srcDirPath.resolve("foo/SomeAnnotation.java")).hasContent(javaSource);
        }

        @Test
        void handles_default_package() {
            String javaSource = """
                class Test {}
                """;

            javaSrcDir.writeClass(javaSource);

            assertThat(srcDirPath.resolve("Test.java")).hasContent(javaSource);
        }

        @Test
        void handles_multiple_nested_package() {
            String javaSource =
                    """
                package foo.bar.baz;
                class Test {}
                """;

            javaSrcDir.writeClass(javaSource);

            assertThat(srcDirPath.resolve("foo/bar/baz/Test.java")).hasContent(javaSource);
        }

        @Test
        void nested_classes_do_not_confuse_it() {
            javaSrcDir.writeClass(
                    """
                package foo;
                class SomeClass {
                    class NestedClass {}
                }
                """);

            assertThat(srcDirPath.resolve("foo/SomeClass.java")).exists();
            assertThat(srcDirPath.resolve("foo/NestedClass.java")).doesNotExist();
        }
    }

    @Nested
    class FileByClass {
        @Test
        void finds_file_by_class_with_package() {
            javaSrcDir
                    .fileByClassName("foo.bar.baz.Test")
                    .assertThat()
                    .isEqualTo(srcDirPath.resolve("foo/bar/baz/Test.java"));
        }

        @Test
        void finds_file_by_class_in_default_package() {
            javaSrcDir.fileByClassName("Test").assertThat().isEqualTo(srcDirPath.resolve("Test.java"));
        }
    }

    @Nested
    class FileByPath {
        @Test
        void finds_file_by_path() {
            javaSrcDir
                    .fileByPath("foo/bar/baz/Test.java")
                    .assertThat()
                    .isEqualTo(srcDirPath.resolve("foo/bar/baz/Test.java"));
        }
    }
}
