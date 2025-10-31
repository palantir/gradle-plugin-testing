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
     * Nested block that can contain child blocks.
     * Children are defined as a list and determine both the available nested blocks and their rendering order.
     */
    public class NestedBlock extends NamedBlock {
        protected final java.util.List<NamedBlock> children;
        private final java.util.Map<String, NamedBlock> childrenByName;

        protected NestedBlock(String blockName, java.util.List<NamedBlock> children) {
            super(blockName);
            this.children = children;
            this.childrenByName = children.stream()
                    .collect(java.util.stream.Collectors.toMap(child -> child.blockName, child -> child, (a, b) -> {
                        throw new IllegalStateException("Duplicate child block name: " + a.blockName);
                    }));
        }

        /**
         * Get a nested child block by name.
         */
        protected NamedBlock nested(String childName) {
            NamedBlock childDef = childrenByName.get(childName);
            if (childDef == null) {
                throw new IllegalStateException(
                        "Child block '%s' not found in nested block '%s'. Available children: %s"
                                .formatted(childName, blockName, childrenByName.keySet()));
            }
            return new NestedChildBlock(blockName, childDef);
        }
    }

    /**
     * Child block nested within a parent block (e.g., repositories within buildscript).
     * Handles recursive nesting by tracking the child definition which may itself be a NestedBlock.
     */
    private final class NestedChildBlock extends NamedBlock {
        private final String parentBlockName;
        private final NamedBlock childDefinition;

        private NestedChildBlock(String parentBlockName, NamedBlock childDefinition) {
            super(childDefinition.blockName);
            this.parentBlockName = parentBlockName;
            this.childDefinition = childDefinition;
        }

        @Override
        public String text() {
            GradleFileState parentState = parseState();
            String parentContent = parentState.getBlock(parentBlockName);

            // Parse parent using its children structure
            NestedBlock parentBlock = findParentBlock(parentBlockName);
            GradleFileState childState = parseWithChildren(parentContent, parentBlock.children);

            return childState.getBlock(blockName);
        }

        @Override
        public NamedBlock edit(ProjectFile.FileEditor editor) {
            GradleFileState parentState = parseState();
            String parentContent = parentState.getBlock(parentBlockName);

            // Parse parent using its children structure
            NestedBlock parentBlock = findParentBlock(parentBlockName);
            GradleFileState childState = parseWithChildren(parentContent, parentBlock.children);

            String currentContent = childState.getBlock(blockName);
            String newContent = editor.edit(currentContent);
            GradleFileState newChildState = childState.withBlock(blockName, newContent);

            String newParentContent = renderWithChildren(newChildState, parentBlock.children);
            GradleFileState newParentState = parentState.withBlock(parentBlockName, newParentContent);

            OrderedGradleFile.this.overwrite(template().render(newParentState));
            return this;
        }

        /**
         * If this child is itself a NestedBlock, provide access to its children.
         */
        protected NamedBlock nested(String grandchildName) {
            if (!(childDefinition instanceof NestedBlock nestedChild)) {
                throw new IllegalStateException("Cannot access nested block '%s' on non-nested block '%s'"
                        .formatted(grandchildName, blockName));
            }
            return nestedChild.nested(grandchildName);
        }

        private NestedBlock findParentBlock(String parentName) {
            // This is a bit hacky - we need to find the parent NestedBlock definition
            // For now, use NestedGradleTemplate as fallback
            // TODO: Could be improved by passing parent definition through constructor
            return new NestedBlock(
                    parentName,
                    java.util.List.of(
                            new NamedBlock("repositories"), new NamedBlock("dependencies"), new NamedBlock("plugins")));
        }

        private GradleFileState parseWithChildren(String content, java.util.List<NamedBlock> children) {
            java.util.List<String> childNames =
                    children.stream().map(child -> child.blockName).collect(java.util.stream.Collectors.toList());
            return template().parseBlocks(content, childNames);
        }

        private String renderWithChildren(GradleFileState state, java.util.List<NamedBlock> children) {
            StringBuilder result = new StringBuilder();
            for (NamedBlock child : children) {
                String blockContent = state.getBlock(child.blockName);
                if (!blockContent.isEmpty()) {
                    if (!result.isEmpty()) {
                        result.append("\n\n");
                    }
                    result.append(template().formatBlock(child.blockName, blockContent));
                }
            }
            if (!state.unstructuredContent().isEmpty()) {
                if (!result.isEmpty()) {
                    result.append("\n\n");
                }
                result.append(state.unstructuredContent());
            }
            return result.toString();
        }
    }
}
