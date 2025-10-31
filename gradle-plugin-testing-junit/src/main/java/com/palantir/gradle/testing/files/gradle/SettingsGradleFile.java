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
import java.util.List;
import org.intellij.lang.annotations.Language;

public final class SettingsGradleFile extends OrderedGradleFile {
    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public SettingsGradleFile(Path path) {
        super(path);
    }

    @Override
    protected GradleFileTemplate template() {
        return SettingsGradleTemplate.INSTANCE;
    }

    public SettingsGradleFile rootProjectName(String rootProjectName) {
        String rootProjectNameLine = "rootProject.name = '%s'".formatted(rootProjectName);

        // Bypass template system to preserve exact formatting
        editWithoutTemplate(text -> {
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

    private void editWithoutTemplate(com.palantir.gradle.testing.files.ProjectFile.FileEditor editor) {
        String text = Files.exists(path()) ? text() : "";
        String newContent = editor.edit(text);
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(
                    path(), newContent, java.nio.charset.StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    public SettingsGradleFile include(String projectPath) {
        @Language("Gradle")
        String includeLine = "include '%s'".formatted(projectPath);

        if (Files.exists(path()) && text().contains(includeLine)) {
            return this;
        }

        appendLine(includeLine);
        return this;
    }

    public PluginManagementBlock pluginManagement() {
        return new PluginManagementBlock();
    }

    public BuildscriptBlock buildscript() {
        return new BuildscriptBlock();
    }

    public NamedBlock plugins() {
        return new NamedBlock("plugins");
    }

    /**
     * PluginManagement block with repositories, plugins, and resolutionStrategy children.
     */
    public final class PluginManagementBlock extends NestedBlock {
        private PluginManagementBlock() {
            super(
                    "pluginManagement",
                    List.of(
                            new NamedBlock("repositories"),
                            new NamedBlock("plugins"),
                            new NamedBlock("resolutionStrategy")));
        }

        public NamedBlock repositories() {
            return nested("repositories");
        }

        public NamedBlock plugins() {
            return nested("plugins");
        }

        public NamedBlock resolutionStrategy() {
            return nested("resolutionStrategy");
        }
    }

    /**
     * Buildscript block with repositories, dependencies, and plugins children.
     */
    public final class BuildscriptBlock extends NestedBlock {
        private BuildscriptBlock() {
            super(
                    "buildscript",
                    List.of(
                            new NamedBlock("repositories"),
                            new NamedBlock("dependencies"),
                            new NamedBlock("plugins")));
        }

        public NamedBlock repositories() {
            return nested("repositories");
        }

        public NamedBlock dependencies() {
            return nested("dependencies");
        }

        public NamedBlock plugins() {
            return nested("plugins");
        }
    }
}
