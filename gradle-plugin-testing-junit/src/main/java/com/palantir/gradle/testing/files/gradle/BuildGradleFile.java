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

/**
 * Represents a Gradle {@code build.gradle} file with structured access to common blocks.
 * <p>
 * Provides typed accessors for standard Gradle build file blocks such as:
 * <ul>
 *   <li>{@link #buildscript()} - Build script configuration</li>
 *   <li>{@link #plugins()} - Plugin declarations</li>
 *   <li>{@link #repositories()} - Repository configuration</li>
 *   <li>{@link #dependencies()} - Project dependencies</li>
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
    List<Block> blocks() {
        return List.of(
                nested(
                        "buildscript",
                        nested("repositories", closureNoMerge("maven")),
                        closure("dependencies"),
                        closure("plugins")),
                closure("plugins"),
                nested("repositories", closureNoMerge("maven")),
                closure("dependencies"));
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
