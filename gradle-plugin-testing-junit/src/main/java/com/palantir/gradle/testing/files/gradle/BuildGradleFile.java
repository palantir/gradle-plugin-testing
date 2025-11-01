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
import com.palantir.gradle.testing.files.gradle.blocks.BlockHandle;
import com.palantir.gradle.testing.files.gradle.blocks.ParsedContent;
import com.palantir.gradle.testing.files.gradle.blocks.Template;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BuildGradleFile implements GradleFile {
    private final Path path;
    private final Template template = BuildGradleTemplate.INSTANCE;

    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public BuildGradleFile(Path path) {
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
    public BuildGradleFile append(String text) {
        // Parse existing content, parse new content, merge, then render
        ParsedContent existing = template.parse(text());
        ParsedContent toAppend = template.parse(text);
        ParsedContent merged = existing.merge(toAppend);

        overwrite(template.render(merged));
        return this;
    }

    @Override
    public BuildGradleFile edit(FileEditor editor) {
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

    public BuildscriptBlock buildscript() {
        return new BuildscriptBlock(this);
    }

    public BlockHandle plugins() {
        return new BlockHandle(this, template, "plugins");
    }

    public BlockHandle repositories() {
        return new BlockHandle(this, template, "repositories");
    }

    public BlockHandle dependencies() {
        return new BlockHandle(this, template, "dependencies");
    }

    public BlockHandle allprojects() {
        return new BlockHandle(this, template, "allprojects");
    }

    public BlockHandle subprojects() {
        return new BlockHandle(this, template, "subprojects");
    }

    public ConfigurationsBlock configurations() {
        return new ConfigurationsBlock(this);
    }

    /**
     * Buildscript block with repositories, dependencies, and plugins children.
     */
    public static final class BuildscriptBlock extends BlockHandle {
        private BuildscriptBlock(BuildGradleFile file) {
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

    /**
     * Configurations block with nested all() that can contain resolutionStrategy.
     */
    public static final class ConfigurationsBlock {
        private final BuildGradleFile file;

        private ConfigurationsBlock(BuildGradleFile file) {
            this.file = file;
        }

        public AllConfigurationBlock all() {
            return new AllConfigurationBlock(file);
        }
    }

    /**
     * All configuration block within configurations that can contain resolutionStrategy.
     */
    public static final class AllConfigurationBlock extends BlockHandle {
        private AllConfigurationBlock(BuildGradleFile file) {
            super(file, file.template, "configurations", "all");
        }

        public BlockHandle resolutionStrategy() {
            return new BlockHandle(root, template, "configurations", "all", "resolutionStrategy");
        }
    }
}
