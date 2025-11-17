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

package com.palantir.gradle.testing.files.gradle;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsGradleFileTest {
    SettingsGradleFile settingsGradleFile;

    @BeforeEach
    void beforeEach(@TempDir Path tempDir) {
        settingsGradleFile = new SettingsGradleFile(tempDir.resolve("settings.gradle"));
    }

    @Nested
    class RootProjectName {
        @Test
        void set_root_project_name() {
            settingsGradleFile.rootProjectName("blahblah");

            settingsGradleFile.assertThat().hasContent("rootProject.name = 'blahblah'\n");
        }

        @Test
        void change_root_project_name() {
            settingsGradleFile.rootProjectName("first");
            settingsGradleFile.rootProjectName("second");

            settingsGradleFile.assertThat().hasContent("rootProject.name = 'second'\n");
            settingsGradleFile.assertThat().content().doesNotContain("first");
        }

        @Test
        void maintains_ending_newlines() {
            settingsGradleFile.append("something already here\n\n");

            settingsGradleFile.rootProjectName("name");

            settingsGradleFile.assertThat().hasContent("something already here\n\nrootProject.name = 'name'\n");
        }

        @Test
        void replaces_insitu() {
            settingsGradleFile.overwrite("""
                // before

                rootProject.name = 'init'

                // after
                """);

            settingsGradleFile.rootProjectName("name");

            settingsGradleFile.assertThat().hasContent("""
                // before

                rootProject.name = 'name'

                // after
                """);
        }

        @Test
        void throws_when_multiple_rootProject_name_assignments_exist() {
            settingsGradleFile.overwrite("""
                // before
                rootProject.name = 'init'
                // during
                rootProject.name = 'second'
                // after
                """);

            assertThatIllegalStateException()
                    .isThrownBy(() -> settingsGradleFile.rootProjectName("name"))
                    .withMessageContaining("Found multiple rootProject.name assignments")
                    .withMessageContaining("use the rootProjectName() method instead");
        }
    }

    @Nested
    class Include {
        @Test
        void including_a_project_path_puts_it_in_the_settings_gradle() {
            settingsGradleFile.include("foo:bar:baz");

            settingsGradleFile.assertThat().content().contains("include 'foo:bar:baz'");
        }

        @Test
        void including_the_same_project_path_multiple_times_does_not_make_multiple_settings_entries() {
            settingsGradleFile.include("foo");
            settingsGradleFile.include("foo");

            settingsGradleFile.assertThat().content().containsOnlyOnce("foo");
        }
    }
}
