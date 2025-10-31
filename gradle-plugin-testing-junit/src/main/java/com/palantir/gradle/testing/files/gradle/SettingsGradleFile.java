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
import com.palantir.gradle.testing.files.gradle.blocks.GradleFileTemplate;
import com.palantir.gradle.testing.files.gradle.blocks.NamedBlock;
import com.palantir.gradle.testing.files.gradle.blocks.SettingsGradleTemplate;
import java.nio.file.Files;
import java.nio.file.Path;
import org.intellij.lang.annotations.Language;

public final class SettingsGradleFile extends OrderedGradleFile {
    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public SettingsGradleFile(Path path) {
        super(path);
    }

    @Override
    protected GradleFileTemplate templateInternal() {
        return SettingsGradleTemplate.INSTANCE;
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

    public NamedBlock plugins() {
        return new NamedBlock(this, "plugins");
    }

    /**
     * PluginManagement block with repositories, plugins, and resolutionStrategy children.
     */
    public static final class PluginManagementBlock extends NamedBlock {
        private PluginManagementBlock(SettingsGradleFile file) {
            super(file, "pluginManagement");
        }

        @Override
        protected java.util.List<String> childBlockOrder() {
            return java.util.List.of("repositories", "plugins", "resolutionStrategy");
        }

        public NamedBlock repositories() {
            return nested("repositories", this);
        }

        public NamedBlock plugins() {
            return nested("plugins", this);
        }

        public NamedBlock resolutionStrategy() {
            return nested("resolutionStrategy", this);
        }
    }

    /**
     * Buildscript block with repositories, dependencies, and plugins children.
     */
    public static final class BuildscriptBlock extends NamedBlock {
        private BuildscriptBlock(SettingsGradleFile file) {
            super(file, "buildscript");
        }

        @Override
        protected java.util.List<String> childBlockOrder() {
            return java.util.List.of("repositories", "dependencies", "plugins");
        }

        public NamedBlock repositories() {
            return nested("repositories", this);
        }

        public NamedBlock dependencies() {
            return nested("dependencies", this);
        }

        public NamedBlock plugins() {
            return nested("plugins", this);
        }
    }
}
