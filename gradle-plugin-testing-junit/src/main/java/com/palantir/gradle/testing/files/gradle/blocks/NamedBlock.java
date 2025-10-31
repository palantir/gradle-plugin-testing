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

package com.palantir.gradle.testing.files.gradle.blocks;

import com.palantir.gradle.testing.files.ProjectFile;
import com.palantir.gradle.testing.files.gradle.GradleFile;
import com.palantir.gradle.testing.files.gradle.OrderedGradleFile;
import java.nio.file.Path;
import java.util.List;

/**
 * Named block that operates on a specific block within a Gradle file.
 * Can optionally have a parent block for nested structures.
 */
public class NamedBlock implements GradleFile {
    private final OrderedGradleFile file;
    protected final String blockName;
    private final NamedBlock parent;

    public NamedBlock(OrderedGradleFile file, String blockName) {
        this(file, blockName, null);
    }

    public NamedBlock(OrderedGradleFile file, String blockName, NamedBlock parent) {
        this.file = file;
        this.blockName = blockName;
        this.parent = parent;
    }

    /**
     * Constructor for nested blocks - inherits file from parent.
     */
    public NamedBlock(NamedBlock parent, String blockName) {
        this(parent.file, blockName, parent);
    }

    @Override
    public final Path path() {
        return file.path();
    }

    @Override
    public String text() {
        if (parent == null) {
            return file.parseState().getBlock(blockName);
        }

        // Nested block - parse parent's content to extract this block
        String parentContent = parent.text();
        GradleFileTemplate nestedTemplate = new NestedGradleTemplate(parent.childBlockOrder());
        GradleFileState state = nestedTemplate.parse(parentContent);
        return state.getBlock(blockName);
    }

    @Override
    public NamedBlock edit(ProjectFile.FileEditor editor) {
        if (parent == null) {
            // Top-level block
            GradleFileState currentState = file.parseState();
            String currentContent = currentState.getBlock(blockName);
            String newContent = editor.edit(currentContent);
            GradleFileState newState = currentState.withBlock(blockName, newContent);
            file.overwrite(file.template().render(newState));
            return this;
        }

        // Nested block - update through parent recursively
        parent.edit(parentContent -> {
            // Parse parent's content using its child block names
            GradleFileTemplate nestedTemplate = new NestedGradleTemplate(parent.childBlockOrder());
            GradleFileState state = nestedTemplate.parse(parentContent);

            // Edit this block's content
            String currentContent = state.getBlock(blockName);
            String newContent = editor.edit(currentContent);
            GradleFileState newState = state.withBlock(blockName, newContent);

            // Render back using the same template
            return nestedTemplate.render(newState);
        });

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

    /**
     * Access a nested child block by name.
     */
    protected NamedBlock nested(String childName, NamedBlock parentBlock) {
        return new NamedBlock(file, childName, parentBlock);
    }

    /**
     * Define the canonical ordering of child blocks for this block.
     * Must be overridden by subclasses that have nested children.
     * Blocks without children should not be subclassed.
     */
    protected List<String> childBlockOrder() {
        throw new UnsupportedOperationException(
                "Block '%s' does not support nested blocks. Override childBlockOrder() if this block should have children."
                        .formatted(blockName));
    }
}
