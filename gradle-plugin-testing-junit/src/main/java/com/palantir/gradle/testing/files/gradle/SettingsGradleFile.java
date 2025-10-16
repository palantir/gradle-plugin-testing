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

import com.google.common.base.Splitter;
import com.google.errorprone.annotations.RestrictedApi;
import com.palantir.gradle.testing.RestrictedCreation;
import java.nio.file.Path;
import java.util.stream.Collectors;
import org.intellij.lang.annotations.Language;

public record SettingsGradleFile(Path path) implements GradleFile {
    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public SettingsGradleFile {}

    public SettingsGradleFile rootProjectName(String rootProjectName) {
        edit(text -> {
            return Splitter.on('\n')
                    .splitToStream(text)
                    .filter(line -> !line.startsWith("rootProject.name"))
                    .collect(Collectors.joining("\n"));
        });

        prependLine("rootProject.name = '%s'".formatted(rootProjectName));

        return this;
    }

    public SettingsGradleFile include(String projectPath) {
        @Language("Gradle")
        String includeLine = "include '%s'".formatted(projectPath);

        if (text().contains(includeLine)) {
            return this;
        }

        appendLine(includeLine);
        return this;
    }
}
