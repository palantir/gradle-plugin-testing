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
import com.palantir.gradle.testing.files.gradle.blocks.BlockHandle;
import com.palantir.gradle.testing.files.gradle.blocks.ParsedContent;
import com.palantir.gradle.testing.files.gradle.blocks.SettingsGradleTemplate;
import com.palantir.gradle.testing.files.gradle.blocks.Template;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.intellij.lang.annotations.Language;

public final class SettingsGradleFile implements GradleFile {
    private final Path path;
    private final Template template = SettingsGradleTemplate.INSTANCE;

    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public SettingsGradleFile(Path path) {
        this.path = path;
    }

    @Override
    public Path path() {
        return path;
    }

    @Override
    public String text() {
        try {
            return Files.exists(path) ? Files.readString(path) : "";
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public SettingsGradleFile append(String text) {
        // Parse existing content, parse new content, merge, then render
        ParsedContent existing = template.parse(text());
        ParsedContent toAppend = template.parse(text);
        ParsedContent merged = existing.merge(toAppend);

        overwrite(template.render(merged));
        return this;
    }

    @Override
    public SettingsGradleFile edit(FileEditor editor) {
        // Parse, edit, reparse, render
        ParsedContent parsed = template.parse(text());
        String edited = editor.edit(template.render(parsed));
        ParsedContent reparsed = template.parse(edited);

        overwrite(template.render(reparsed));
        return this;
    }

    Template template() {
        return template;
    }

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
        return new PluginManagementBlock(this);
    }

    public BuildscriptBlock buildscript() {
        return new BuildscriptBlock(this);
    }

    public BlockHandle plugins() {
        return new BlockHandle(this, template, "plugins");
    }

    /**
     * PluginManagement block with repositories, plugins, and resolutionStrategy children.
     */
    public static final class PluginManagementBlock extends BlockHandle {
        private PluginManagementBlock(SettingsGradleFile file) {
            super(file, file.template, "pluginManagement");
        }

        public BlockHandle repositories() {
            return new BlockHandle(root, template, "pluginManagement", "repositories");
        }

        public BlockHandle plugins() {
            return new BlockHandle(root, template, "pluginManagement", "plugins");
        }

        public BlockHandle resolutionStrategy() {
            return new BlockHandle(root, template, "pluginManagement", "resolutionStrategy");
        }
    }

    /**
     * Buildscript block with repositories, dependencies, and plugins children.
     */
    public static final class BuildscriptBlock extends BlockHandle {
        private BuildscriptBlock(SettingsGradleFile file) {
            super(file, file.template, "buildscript");
        }

        public BlockHandle repositories() {
            return new BlockHandle(root, template, "buildscript", "repositories");
        }

        public BlockHandle dependencies() {
            return new BlockHandle(root, template, "buildscript", "dependencies");
        }

        public BlockHandle plugins() {
            return new BlockHandle(root, template, "buildscript", "plugins");
        }
    }
}
