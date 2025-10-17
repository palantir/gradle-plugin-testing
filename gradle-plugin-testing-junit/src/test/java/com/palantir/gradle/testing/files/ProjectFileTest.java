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

package com.palantir.gradle.testing.files;

import com.palantir.gradle.testing.files.arbitrary.ArbitraryFile;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectFileTest {
    ProjectFile<?> projectFile;

    @BeforeEach
    void beforeEach(@TempDir Path tempDir) {
        projectFile = new ArbitraryFile(tempDir.resolve("file"));
    }

    @Nested
    class Overwrite {
        @Test
        void create_a_new_file_with_contents() {
            projectFile.overwrite("contents");

            projectFile.assertThat().hasContent("contents");
        }

        @Test
        void overwrite_existing_contents() {
            projectFile.overwrite("first");
            projectFile.overwrite("second");

            projectFile.assertThat().hasContent("second");
        }
    }

    @Nested
    class Append {
        @Test
        void create_a_new_file_with_contents() {
            projectFile.append("contents");

            projectFile.assertThat().hasContent("contents");
        }

        @Test
        void append_to_existing_contents() {
            projectFile.overwrite("first");
            projectFile.append("second");

            projectFile.assertThat().hasContent("firstsecond");
        }
    }

    @Nested
    class AppendLine {
        @Test
        void create_a_new_file_with_text_plus_newline() {
            projectFile.appendLine("contents");

            projectFile.assertThat().hasContent("contents\n");
        }

        @Test
        void appends_line_to_existing_contents() {
            projectFile.overwrite("existing\n");
            projectFile.appendLine("new line");

            projectFile.assertThat().hasContent("existing\nnew line\n");
        }
    }

    @Nested
    class Prepend {
        @Test
        void create_a_new_file_with_contents() {
            projectFile.prepend("contents");

            projectFile.assertThat().hasContent("contents");
        }

        @Test
        void prepend_to_existing_contents() {
            projectFile.overwrite("existing");
            projectFile.prepend("prepended");

            projectFile.assertThat().hasContent("prependedexisting");
        }
    }

    @Nested
    class PrependLine {
        @Test
        void create_a_new_file_with_text_plus_newline() {
            projectFile.prependLine("contents");

            projectFile.assertThat().hasContent("contents\n");
        }

        @Test
        void prepends_line_to_existing_contents() {
            projectFile.overwrite("existing");
            projectFile.prependLine("new line");

            projectFile.assertThat().hasContent("new line\nexisting");
        }
    }

    @Nested
    class Edit {
        @Test
        void edit_existing_file_contents() {
            projectFile.overwrite("original text");

            projectFile.edit(text -> text.replace("original", "modified"));

            projectFile.assertThat().hasContent("modified text");
        }

        @Test
        void create_new_file_with_edited_content() {
            projectFile.edit(text -> "new content");

            projectFile.assertThat().hasContent("new content");
        }
    }

    @Nested
    class CreateEmpty {
        @Test
        void creates_empty_file() {
            projectFile.createEmpty();

            projectFile.assertThat().exists().hasContent("");
        }

        @Test
        void overwrites_existing_file_with_empty_content() {
            projectFile.overwrite("some content");
            projectFile.createEmpty();

            projectFile.assertThat().hasContent("");
        }
    }
}
