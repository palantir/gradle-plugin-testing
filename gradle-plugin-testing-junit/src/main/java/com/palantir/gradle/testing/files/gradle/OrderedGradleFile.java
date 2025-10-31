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

import com.palantir.gradle.testing.files.ProjectFile;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Base class for Gradle files with structured blocks that follow canonical ordering.
 * Provides template-based parsing and rendering to maintain consistent block order.
 */
public abstract class OrderedGradleFile implements GradleFile {
    private final Path path;

    protected OrderedGradleFile(Path path) {
        this.path = path;
    }

    @Override
    public final Path path() {
        return path;
    }

    @Override
    public String text() {
        if (!Files.exists(path())) {
            return "";
        }
        return GradleFile.super.text();
    }

    @Override
    public GradleFile edit(ProjectFile.FileEditor editor) {
        GradleFileState currentState = parseState();
        String editedContent = editor.edit(template().render(currentState));
        GradleFileState newState = template().parse(editedContent);
        overwrite(template().render(newState));
        return this;
    }

    protected final GradleFileState parseState() {
        return Files.exists(path()) ? template().parse(text()) : GradleFileState.empty();
    }

    /**
     * Returns the template used for parsing and rendering this file.
     */
    protected abstract GradleFileTemplate template();

    /**
     * Named block that operates on a specific block within the file.
     */
    public class NamedBlock implements GradleFile {
        protected final String blockName;

        protected NamedBlock(String blockName) {
            this.blockName = blockName;
        }

        @Override
        public Path path() {
            return OrderedGradleFile.this.path();
        }

        @Override
        public String text() {
            return parseState().getBlock(blockName);
        }

        @Override
        public NamedBlock edit(ProjectFile.FileEditor editor) {
            GradleFileState currentState = parseState();
            String currentContent = currentState.getBlock(blockName);
            String newContent = editor.edit(currentContent);
            GradleFileState newState = currentState.withBlock(blockName, newContent);
            OrderedGradleFile.this.overwrite(template().render(newState));
            return this;
        }

        @Override
        public NamedBlock overwrite(String content) {
            return edit(_existing -> content);
        }

        @Override
        public NamedBlock append(String content) {
            return edit(existing -> {
                if (existing.isEmpty()) {
                    return content;
                }
                return existing + "\n" + content;
            });
        }

        @Override
        public NamedBlock prepend(String content) {
            return edit(existing -> {
                if (existing.isEmpty()) {
                    return content;
                }
                return content + "\n" + existing;
            });
        }
    }

    /**
     * Nested block that can contain child blocks (repositories, dependencies, plugins).
     */
    public class NestedBlock extends NamedBlock {
        protected NestedBlock(String blockName) {
            super(blockName);
        }

        public NamedBlock repositories() {
            return new NestedChildBlock(blockName, "repositories");
        }

        public NamedBlock dependencies() {
            return new NestedChildBlock(blockName, "dependencies");
        }

        public NamedBlock plugins() {
            return new NestedChildBlock(blockName, "plugins");
        }
    }

    /**
     * Child block nested within a parent block (e.g., repositories within buildscript).
     */
    private final class NestedChildBlock extends NamedBlock {
        private final String parentBlockName;

        private NestedChildBlock(String parentBlockName, String childBlockName) {
            super(childBlockName);
            this.parentBlockName = parentBlockName;
        }

        @Override
        public String text() {
            GradleFileState parentState = parseState();
            String parentContent = parentState.getBlock(parentBlockName);
            GradleFileState childState = NestedGradleTemplate.INSTANCE.parse(parentContent);
            return childState.getBlock(blockName);
        }

        @Override
        public NamedBlock edit(ProjectFile.FileEditor editor) {
            GradleFileState parentState = parseState();
            String parentContent = parentState.getBlock(parentBlockName);
            GradleFileState childState = NestedGradleTemplate.INSTANCE.parse(parentContent);

            String currentContent = childState.getBlock(blockName);
            String newContent = editor.edit(currentContent);
            GradleFileState newChildState = childState.withBlock(blockName, newContent);

            String newParentContent = NestedGradleTemplate.INSTANCE.render(newChildState);
            GradleFileState newParentState = parentState.withBlock(parentBlockName, newParentContent);

            OrderedGradleFile.this.overwrite(template().render(newParentState));
            return this;
        }
    }
}
