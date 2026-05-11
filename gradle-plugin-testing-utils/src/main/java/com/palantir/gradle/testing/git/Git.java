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
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Git {

    public static String run(File directory, String... commands) throws IOException, InterruptedException {
        return run(directory, Map.of(), commands);
    }

    public static String run(File directory, Map<String, String> environment, String... commands)
            throws IOException, InterruptedException {
        List<String> commandArguments =
                ImmutableList.<String>builder().add("git").add(commands).build();
        ProcessBuilder processBuilder = new ProcessBuilder()
                .command(commandArguments)
                .redirectErrorStream(true)
                .directory(directory);
        processBuilder.environment().putAll(environment);

        Process process = processBuilder.start();
        String output = readAllInput(process.getInputStream());
        assertThat(process.waitFor())
                .as("Command '%s' failed with output: %s", String.join(" ", commandArguments), output)
                .isEqualTo(0);
        return output;
    }

    public static void init(File projectDir) throws IOException, InterruptedException {
        run(projectDir, "init", projectDir.toString());

        // So git doesn't ask you to gpgsign when running tests locally
        run(projectDir, "config", "commit.gpgsign", "false");
        run(projectDir, "config", "tag.gpgsign", "false");
        run(projectDir, "config", "tag.forcesignannotated", "false");

        run(projectDir, "config", "user.email", "email@example.com");
        run(projectDir, "config", "user.name", "name");
    }

    private static String readAllInput(InputStream inputStream) {
        try (Stream<String> lines =
                new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()) {
            return lines.collect(Collectors.joining("\n"));
        }
    }

    private Git() {}
}
