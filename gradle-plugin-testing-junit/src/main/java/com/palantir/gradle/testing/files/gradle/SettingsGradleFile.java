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

import com.google.errorprone.annotations.RestrictedApi;
import com.palantir.gradle.testing.RestrictedCreation;
import java.nio.file.Files;
import java.nio.file.Path;
import org.intellij.lang.annotations.Language;

public record SettingsGradleFile(Path path) implements GradleFile {
    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public SettingsGradleFile {}

    public SettingsGradleFile rootProjectName(String rootProjectName) {
        String rootProjectNameLine = "rootProject.name = '%s'".formatted(rootProjectName);

        edit(text -> {
            long count = text.lines()
                    .filter(line -> line.matches("rootProject\\.name[^\\n]*"))
                    .count();

            if (count > 1) {
                throw new IllegalStateException("Found multiple rootProject.name assignments in settings.gradle. "
                        + "Please remove the duplicate assignments and use the rootProjectName() method instead.");
            }

            return text.contains("rootProject.name")
                    ? text.replaceFirst("rootProject\\.name[^\\n]*", rootProjectNameLine)
                    : text + rootProjectNameLine + "\n";
        });

        return this;
    }

    public SettingsGradleFile includeBuild(String buildPath) {
        return idempotentAppend("includeBuild '%s'".formatted(buildPath));
    }

    public SettingsGradleFile include(String projectPath) {
        return idempotentAppend("include '%s'".formatted(projectPath));
    }

    private SettingsGradleFile idempotentAppend(@Language("Gradle") String line) {
        if (Files.exists(path) && text().contains(line)) {
            return this;
        }

        appendLine(line);
        return this;
    }
}
