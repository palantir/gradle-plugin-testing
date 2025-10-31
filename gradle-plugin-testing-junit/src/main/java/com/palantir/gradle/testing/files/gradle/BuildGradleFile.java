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

public final class BuildGradleFile extends OrderedGradleFile {
    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public BuildGradleFile(Path path) {
        super(path);
    }

    @Override
    protected GradleFileTemplate template() {
        return BuildGradleTemplate.INSTANCE;
    }

    public BuildscriptBlock buildscript() {
        return new BuildscriptBlock();
    }

    public NamedBlock plugins() {
        return new NamedBlock("plugins");
    }

    public NamedBlock repositories() {
        return new NamedBlock("repositories");
    }

    public NamedBlock dependencies() {
        return new NamedBlock("dependencies");
    }

    public NamedBlock allprojects() {
        return new NamedBlock("allprojects");
    }

    public NamedBlock subprojects() {
        return new NamedBlock("subprojects");
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
