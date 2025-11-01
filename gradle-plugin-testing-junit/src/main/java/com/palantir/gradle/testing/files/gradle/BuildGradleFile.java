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
import com.palantir.gradle.testing.files.gradle.blocks.GradleBlock;
import java.nio.file.Path;
import java.util.List;

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

    public GradleBlock plugins() {
        return new GradleBlock(this, "plugins");
    }

    public GradleBlock repositories() {
        return new GradleBlock(this, "repositories");
    }

    public GradleBlock dependencies() {
        return new GradleBlock(this, "dependencies");
    }

    public GradleBlock allprojects() {
        return new GradleBlock(this, "allprojects");
    }

    public GradleBlock subprojects() {
        return new GradleBlock(this, "subprojects");
    }

    public ConfigurationsBlock configurations() {
        return new ConfigurationsBlock(this, "configurations");
    }

    /**
     * Buildscript block with repositories, dependencies, and plugins children.
     */
    public static final class BuildscriptBlock extends GradleBlock {
        private BuildscriptBlock(BuildGradleFile file, String... path) {
            super(file, path);
        }

        public GradleBlock repositories() {
            return new GradleBlock(getRoot(), concat(getBlockPath(), "repositories"));
        }

        public GradleBlock dependencies() {
            return new GradleBlock(getRoot(), concat(getBlockPath(), "dependencies"));
        }

        public GradleBlock plugins() {
            return new GradleBlock(getRoot(), concat(getBlockPath(), "plugins"));
        }
    }

    /**
     * Configurations block with nested all() that can contain resolutionStrategy.
     */
    public static final class ConfigurationsBlock extends GradleBlock {
        private ConfigurationsBlock(BuildGradleFile file, String... path) {
            super(file, path);
        }

        public AllConfigurationBlock all() {
            return new AllConfigurationBlock(getRoot(), concat(getBlockPath(), "all"));
        }
    }

    /**
     * All configuration block within configurations that can contain resolutionStrategy.
     */
    public static final class AllConfigurationBlock extends GradleBlock {
        private AllConfigurationBlock(StructuredGradleFile root, String... path) {
            super(root, path);
        }

        public GradleBlock resolutionStrategy() {
            return new GradleBlock(getRoot(), concat(getBlockPath(), "resolutionStrategy"));
        }
    }
}
