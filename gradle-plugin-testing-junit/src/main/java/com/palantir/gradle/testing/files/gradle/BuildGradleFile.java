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
import com.palantir.gradle.testing.files.gradle.blocks.BlockEditor;
import java.nio.file.Path;
import java.util.List;

/**
 * Represents a Gradle {@code build.gradle} file with structured access to common blocks.
 * <p>
 * Provides typed accessors for standard Gradle build file blocks such as:
 * <ul>
 *   <li>{@link #buildscript()} - Build script configuration</li>
 *   <li>{@link #plugins()} - Plugin declarations</li>
 *   <li>{@link #repositories()} - Repository configuration</li>
 *   <li>{@link #dependencies()} - Project dependencies</li>
 *   <li>{@link #configurations()} - Dependency configurations</li>
 * </ul>
 *
 * @see StructuredGradleFile
 * @see BlockEditor
 */
public final class BuildGradleFile extends StructuredGradleFile {

    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public BuildGradleFile(Path path) {
        super(path);
    }

    @Override
    protected List<Block> blocks() {
        return List.of(
                nested("buildscript", closure("repositories"), closure("dependencies"), closure("plugins")),
                closure("plugins"),
                closure("allprojects"),
                closure("subprojects"),
                closure("repositories"),
                closure("dependencies"),
                nested("configurations", nested("all", closure("resolutionStrategy"))));
    }

    public BuildscriptBlock buildscript() {
        return new BuildscriptBlock(this, "buildscript");
    }

    public BlockEditor plugins() {
        return new BlockEditor(this, "plugins");
    }

    public BlockEditor repositories() {
        return new BlockEditor(this, "repositories");
    }

    public BlockEditor dependencies() {
        return new BlockEditor(this, "dependencies");
    }

    public BlockEditor allprojects() {
        return new BlockEditor(this, "allprojects");
    }

    public BlockEditor subprojects() {
        return new BlockEditor(this, "subprojects");
    }

    public ConfigurationsBlock configurations() {
        return new ConfigurationsBlock(this, "configurations");
    }

    /**
     * {@code buildscript} block with child blocks for repositories, dependencies, and plugins.
     * <p>
     * Used to configure the build script classpath.
     *
     * @see BlockEditor
     */
    public static final class BuildscriptBlock extends BlockEditor {
        private BuildscriptBlock(BuildGradleFile file, String... path) {
            super(file, path);
        }

        public BlockEditor repositories() {
            return new BlockEditor(getRoot(), concat(getBlockPath(), "repositories"));
        }

        public BlockEditor dependencies() {
            return new BlockEditor(getRoot(), concat(getBlockPath(), "dependencies"));
        }

        public BlockEditor plugins() {
            return new BlockEditor(getRoot(), concat(getBlockPath(), "plugins"));
        }
    }

    /**
     * {@code configurations} block with nested configuration blocks.
     * <p>
     * Provides access to dependency configuration settings.
     *
     * @see BlockEditor
     */
    public static final class ConfigurationsBlock extends BlockEditor {
        private ConfigurationsBlock(BuildGradleFile file, String... path) {
            super(file, path);
        }

        public AllConfigurationBlock all() {
            return new AllConfigurationBlock(getRoot(), concat(getBlockPath(), "all"));
        }
    }

    /**
     * {@code all} configuration block within {@code configurations}.
     * <p>
     * Used to configure resolution strategy and other settings for all configurations.
     *
     * @see BlockEditor
     */
    public static final class AllConfigurationBlock extends BlockEditor {
        private AllConfigurationBlock(StructuredGradleFile root, String... path) {
            super(root, path);
        }

        public BlockEditor resolutionStrategy() {
            return new BlockEditor(getRoot(), concat(getBlockPath(), "resolutionStrategy"));
        }
    }
}
