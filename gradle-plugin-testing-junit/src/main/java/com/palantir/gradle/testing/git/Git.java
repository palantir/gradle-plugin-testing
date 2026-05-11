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

package com.palantir.gradle.testing.git;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.RestrictedApi;
import com.palantir.gradle.testing.RestrictedCreation;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A git repository in the root project directory, auto-initialised on injection and isolated from the host's git
 * config. Shells out to the {@code git} binary on {@code PATH}.
 *
 * <p>Usage:
 * <pre>
 * &#64;Test
 * void resolves_version_from_tag(Git git, GradleInvoker gradle) {
 *     git.commit("initial");
 *     git.tag("1.0.0");
 *
 *     assertThat(gradle.withArgs("printVersion").buildsSuccessfully())
 *         .output().contains("1.0.0");
 * }
 * </pre>
 */
public record Git(Path projectDir) {

    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public Git(Path projectDir) {
        this.projectDir = projectDir;
        run("init", projectDir.toString());
        run("config", "commit.gpgsign", "false");
        run("config", "tag.gpgsign", "false");
        run("config", "tag.forcesignannotated", "false");
        run("config", "user.email", "email@example.com");
        run("config", "user.name", "name");
    }

    /**
     * Creates an empty commit with the given message.
     */
    public void commit(String message) {
        commit(message, Map.of());
    }

    /**
     * Creates an empty commit with the given message and environment variables (e.g. {@code GIT_AUTHOR_DATE}).
     */
    public void commit(String message, Map<String, String> environment) {
        run(environment, "commit", "--allow-empty", "-m", message);
    }

    /**
     * Creates a tag pointing at {@code HEAD}.
     */
    public void tag(String name) {
        run("tag", name);
    }

    /**
     * Runs {@code git} with the given arguments in the project directory and returns the combined stdout/stderr.
     */
    public String run(String... arguments) {
        return run(Map.of(), arguments);
    }

    /**
     * Runs {@code git} with the given environment and arguments in the project directory and returns the combined
     * stdout/stderr.
     */
    public String run(Map<String, String> environment, String... arguments) {
        List<String> commandArguments =
                ImmutableList.<String>builder().add("git").add(arguments).build();
        ProcessBuilder processBuilder = new ProcessBuilder()
                .command(commandArguments)
                .redirectErrorStream(true)
                .directory(projectDir.toFile());
        processBuilder.environment().putAll(environment);

        try {
            Process process = processBuilder.start();
            String output = readAllInput(process.getInputStream());
            assertThat(process.waitFor())
                    .as("Command '%s' failed with output: %s", String.join(" ", commandArguments), output)
                    .isEqualTo(0);
            return output;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while running: " + String.join(" ", commandArguments), e);
        }
    }

    private static String readAllInput(InputStream inputStream) {
        try (Stream<String> lines =
                new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()) {
            return lines.collect(Collectors.joining("\n"));
        }
    }
}
