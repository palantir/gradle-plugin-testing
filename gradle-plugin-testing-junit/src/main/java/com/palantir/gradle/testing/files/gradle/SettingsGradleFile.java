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
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Represents a Gradle {@code settings.gradle} file with structured access to common blocks.
 * <p>
 * Provides typed accessors for standard Gradle settings file blocks such as:
 * <ul>
 * a *   <li>{@link #buildscript()} - Build script configuration</li>
 *   <li>{@link #plugins()} - Plugin declarations</li>
 *   <li>{@link #rootProjectName(String)} - Root project name property</li>
 *   <li>{@link #include(String)} - Include subprojects</li>
 * </ul>
 *
 * @see StructuredGradleFile
 * @see BlockEditor
 */
public final class SettingsGradleFile extends StructuredGradleFile {

    private static final StatementBlock ROOT_PROJECT_NAME =
            new StatementBlock("rootProject.name", "rootProject.name =", Set.of());

    private static final StatementBlock INCLUDE = new StatementBlock("includes", "include", Set.of());

    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public SettingsGradleFile(Path path) {
        super(path);
    }

    @Override
    List<Block> blocks() {
        return List.of(
                nested("buildscript", closure("repositories"), closure("dependencies"), closure("plugins")),
                closure("plugins"),
                ROOT_PROJECT_NAME,
                INCLUDE);
    }

    public BuildscriptBlock buildscript() {
        return new BuildscriptBlock(this, "buildscript");
    }

    public BlockEditor plugins() {
        return new BlockEditor(this, "plugins");
    }

    /**
     * Set the root project name in the settings file.
     *
     * @param rootProjectName the name for the root project
     * @return this {@link SettingsGradleFile} for chaining
     * @throws IllegalStateException if multiple {@code rootProject.name} assignments exist
     */
    public SettingsGradleFile rootProjectName(String rootProjectName) {
        long count = text().lines()
                .filter(line -> line.matches(".*rootProject\\.name.*"))
                .count();

        if (count > 1) {
            throw new IllegalStateException("Found multiple rootProject.name assignments in settings.gradle. "
                    + "Please remove the duplicate assignments and use the rootProjectName() method instead.");
        }

        new BlockEditor(this, "rootProject.name")
                .overwrite(ROOT_PROJECT_NAME.withStatements(rootProjectName).renderBlock());
        return this;
    }

    /**
     * Include a subproject in the build.
     * <p>
     * Adds an {@code include 'projectPath'} line if not already present.
     *
     * @param projectPath the path to the subproject (e.g., {@code "subproject"}, {@code ":sub:nested"})
     * @return this {@link SettingsGradleFile} for chaining
     */
    public SettingsGradleFile include(String projectPath) {
        new BlockEditor(this, "includes")
                .append(INCLUDE.withStatements(projectPath).renderBlock());
        return this;
    }

    /**
     * {@code buildscript} block with child blocks for repositories, dependencies, and plugins.
     * <p>
     * Used to configure the settings script classpath.
     *
     * @see BlockEditor
     */
    public static final class BuildscriptBlock extends BlockEditor {
        private BuildscriptBlock(SettingsGradleFile file, String... path) {
            super(file, path);
        }

        public BlockEditor repositories() {
            return new BlockEditor(root(), concat(blockPath(), "repositories"));
        }

        public BlockEditor dependencies() {
            return new BlockEditor(root(), concat(blockPath(), "dependencies"));
        }

        public BlockEditor plugins() {
            return new BlockEditor(root(), concat(blockPath(), "plugins"));
        }
    }
}
