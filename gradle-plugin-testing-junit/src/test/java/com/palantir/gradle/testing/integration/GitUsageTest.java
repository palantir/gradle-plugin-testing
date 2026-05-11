/*
 * (c) Copyright 2026 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.gradle.testing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.gradle.testing.git.Git;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import java.util.Map;
import org.junit.jupiter.api.Test;

@GradlePluginTests
class GitUsageTest {

    @Test
    void injects_initialized_repo(Git git, RootProject root) {
        assertThat(root.path().resolve(".git")).as("git init ran on injection").exists();
        assertThat(git.run("status")).contains("No commits yet");
    }

    @Test
    void commit_helper_creates_empty_commit(Git git) {
        git.commit("initial");

        assertThat(git.run("log", "--oneline")).contains("initial");
    }

    @Test
    void tag_helper_points_at_head(Git git) {
        git.commit("initial");
        git.tag("1.0.0");

        assertThat(git.run("describe", "--tags").trim()).isEqualTo("1.0.0");
    }

    @Test
    void commit_with_env_vars(Git git) {
        git.commit("authored commit", Map.of("GIT_AUTHOR_NAME", "Overridden Author"));

        assertThat(git.run("log", "-1", "--format=%an").trim()).isEqualTo("Overridden Author");
    }

    @Test
    void multiple_git_params_resolve_to_same_repo(Git git1, Git git2) {
        git1.commit("from git1");
        assertThat(git2.run("log", "--oneline")).contains("from git1");
        assertThat(git1).isSameAs(git2);
    }

    @Test
    void raw_run_escape_hatch_supports_branching(Git git) {
        git.commit("initial");
        git.run("checkout", "-b", "feature");

        assertThat(git.run("branch", "--show-current").trim()).isEqualTo("feature");
    }
}
