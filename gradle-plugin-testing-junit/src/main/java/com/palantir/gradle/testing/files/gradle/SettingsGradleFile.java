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
import com.palantir.gradle.testing.files.gradle.blocks.Block;
import com.palantir.gradle.testing.files.gradle.blocks.ClosureBlock;
import com.palantir.gradle.testing.files.gradle.blocks.GradleBlock;
import com.palantir.gradle.testing.files.gradle.blocks.NestedClosureBlock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.intellij.lang.annotations.Language;

public final class SettingsGradleFile extends StructuredGradleFile {

    private static final Block PLUGIN_MANAGEMENT = new NestedClosureBlock(
            "pluginManagement",
            List.of("repositories", "plugins", "resolutionStrategy"),
            Map.of(
                    "repositories", new ClosureBlock("repositories", ""),
                    "plugins", new ClosureBlock("plugins", ""),
                    "resolutionStrategy", new ClosureBlock("resolutionStrategy", "")),
            "");

    private static final Block BUILDSCRIPT = new NestedClosureBlock(
            "buildscript",
            List.of("repositories", "dependencies", "plugins"),
            Map.of(
                    "repositories", new ClosureBlock("repositories", ""),
                    "dependencies", new ClosureBlock("dependencies", ""),
                    "plugins", new ClosureBlock("plugins", "")),
            "");

    private static final Block PLUGINS = new ClosureBlock("plugins", "");

    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public SettingsGradleFile(Path path) {
        super(path);
    }

    @Override
    protected List<Block> blocks() {
        return List.of(PLUGIN_MANAGEMENT, BUILDSCRIPT, PLUGINS);
    }

    @Override
    protected List<String> blockOrder() {
        return List.of("pluginManagement", "buildscript", "plugins");
    }

    public PluginManagementBlock pluginManagement() {
        return new PluginManagementBlock(this, "pluginManagement");
    }

    public BuildscriptBlock buildscript() {
        return new BuildscriptBlock(this, "buildscript");
    }

    public GradleBlock plugins() {
        return new GradleBlock(this, "plugins");
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

    /**
     * PluginManagement block with repositories, plugins, and resolutionStrategy children.
     */
    public static final class PluginManagementBlock extends GradleBlock {
        private PluginManagementBlock(SettingsGradleFile file, String... path) {
            super(file, path);
        }

        public GradleBlock repositories() {
            return new GradleBlock(root, concat(blockPath, "repositories"));
        }

        public GradleBlock plugins() {
            return new GradleBlock(root, concat(blockPath, "plugins"));
        }

        public GradleBlock resolutionStrategy() {
            return new GradleBlock(root, concat(blockPath, "resolutionStrategy"));
        }
    }

    /**
     * Buildscript block with repositories, dependencies, and plugins children.
     */
    public static final class BuildscriptBlock extends GradleBlock {
        private BuildscriptBlock(SettingsGradleFile file, String... path) {
            super(file, path);
        }

        public GradleBlock repositories() {
            return new GradleBlock(root, concat(blockPath, "repositories"));
        }

        public GradleBlock dependencies() {
            return new GradleBlock(root, concat(blockPath, "dependencies"));
        }

        public GradleBlock plugins() {
            return new GradleBlock(root, concat(blockPath, "plugins"));
        }
    }
}
