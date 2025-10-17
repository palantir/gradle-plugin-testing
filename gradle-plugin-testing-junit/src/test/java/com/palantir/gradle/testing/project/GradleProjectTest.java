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

package com.palantir.gradle.testing.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GradleProjectTest {
    GradleProject gradleProject;

    @BeforeEach
    void beforeEach(@TempDir Path tempDir) {
        gradleProject = new RootProject(tempDir);
    }

    @Nested
    class SubProjectCreation {
        @Test
        void creates_a_subproject() {
            SubProject subProject = gradleProject.subproject("sub");

            assertThat(subProject.path()).isEqualTo(gradleProject.path().resolve("sub"));
        }

        @Test
        void explodes_if_the_subproject_contains_colons() {
            assertThatThrownBy(() -> gradleProject.subproject("sub:name"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sub:name");
        }

        @Test
        void explodes_if_the_subproject_contains_slashes() {
            assertThatThrownBy(() -> gradleProject.subproject("sub/name"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sub/name");
        }
    }
}
