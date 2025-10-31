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
import com.palantir.gradle.testing.files.gradle.blocks.BuildGradleTemplate;
import com.palantir.gradle.testing.files.gradle.blocks.GradleFileTemplate;
import com.palantir.gradle.testing.files.gradle.blocks.NamedBlock;
import com.palantir.gradle.testing.files.gradle.blocks.NestedBlock;
import java.nio.file.Path;
import java.util.List;

public final class BuildGradleFile extends OrderedGradleFile {
    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public BuildGradleFile(Path path) {
        super(path);
    }

    @Override
    protected GradleFileTemplate templateInternal() {
        return BuildGradleTemplate.INSTANCE;
    }

    public BuildscriptBlock buildscript() {
        return new BuildscriptBlock(this);
    }

    public NamedBlock plugins() {
        return new NamedBlock(this, "plugins");
    }

    public NamedBlock repositories() {
        return new NamedBlock(this, "repositories");
    }

    public NamedBlock dependencies() {
        return new NamedBlock(this, "dependencies");
    }

    public NamedBlock allprojects() {
        return new NamedBlock(this, "allprojects");
    }

    public NamedBlock subprojects() {
        return new NamedBlock(this, "subprojects");
    }

    public ConfigurationsBlock configurations() {
        return new ConfigurationsBlock(this);
    }

    /**
     * Buildscript block with repositories, dependencies, and plugins children.
     */
    public static final class BuildscriptBlock extends NestedBlock {
        private BuildscriptBlock(BuildGradleFile file) {
            super(file, "buildscript");
        }

        @Override
        protected List<String> childBlockOrder() {
            return List.of("repositories", "dependencies", "plugins");
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

    /**
     * Configurations block with nested all() that can contain resolutionStrategy.
     */
    public static final class ConfigurationsBlock extends NestedBlock {
        private ConfigurationsBlock(BuildGradleFile file) {
            super(file, "configurations");
        }

        @Override
        protected List<String> childBlockOrder() {
            return List.of("all");
        }

        public AllConfigurationBlock all() {
            return new AllConfigurationBlock(this);
        }
    }

    /**
     * All configuration block within configurations that can contain resolutionStrategy.
     */
    public static final class AllConfigurationBlock extends NestedBlock {
        private AllConfigurationBlock(ConfigurationsBlock parent) {
            super(parent, "all");
        }

        @Override
        protected List<String> childBlockOrder() {
            return List.of("resolutionStrategy");
        }

        public NamedBlock resolutionStrategy() {
            return nested("resolutionStrategy");
        }
    }
}
