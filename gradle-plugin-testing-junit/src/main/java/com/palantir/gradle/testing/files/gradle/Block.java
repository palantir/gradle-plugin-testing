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

import java.util.Optional;
import java.util.function.Function;

/**
 * A structural unit in a Gradle file that can be parsed, rendered, merged, and edited.
 * <p>
 * Blocks are self-contained - each block knows its own pattern, parsing rules, and rendering logic.
 * This enables uniform treatment of different block types ({@link ClosureBlock}, {@link StatementBlock}) without
 * requiring external configuration or templates.
 * <p>
 * Examples:
 * <ul>
 *   <li>{@code plugins { }} - simple closure block</li>
 *   <li>{@code buildscript { repositories { } }} - nested closure block</li>
 *   <li>{@code version = '1.0'} - property block</li>
 * </ul>
 *
 * @see ClosureBlock
 * @see StatementBlock
 * @see ParsedContent
 */
sealed interface Block permits ClosureBlock, StatementBlock {
    /**
     * Parse raw content into a {@link Block} instance.
     *
     * @param content the text that appears inside the block (without wrapper syntax)
     * @return a new {@link Block} containing the parsed content
     */
    Block parse(String content);

    /**
     * Render this block's inner content (without wrapper syntax).
     * @return the content that appears inside the block
     */
    String renderContent();

    /**
     * Render the complete block including wrapper syntax (name, braces, indentation).
     * @return the full block as it should appear in the file
     */
    String renderBlock();

    /**
     * Merge another block's content into this block.
     *
     * @param other the block to merge
     * @return a new {@link Block} containing both this block's content and the other's content
     */
    Block merge(Block other);

    /**
     * Transform this block's content using an editor function.
     *
     * @param editor function that receives current content and returns new content
     * @return a new {@link Block} with the transformed content
     */
    default Block edit(Function<String, String> editor) {
        return parse(editor.apply(renderContent()));
    }

    /**
     * Get a child block by name. Only meaningful for blocks with children.
     * @param childName the name of the child block
     * @return the child block, or empty if not found or this block doesn't support children
     */
    Optional<Block> getChild(String childName);

    /**
     * Create a new block with an updated child. Only meaningful for blocks with children.
     *
     * @param childName the name of the child to update
     * @param child the new child block
     * @return a new {@link Block} with the child updated
     * @throws UnsupportedOperationException if this block doesn't support children
     */
    Block withChild(String childName, Block child);

    /**
     * The identifier for this block.
     *
     * @return block name (e.g., {@code "plugins"}, {@code "repositories"}, {@code "buildscript"})
     */
    String name();

    /**
     * Whether this block should merge with other blocks of the same name.
     * <p>
     * When {@code false}, multiple blocks with the same name will be kept separate rather than merged.
     * This is useful for blocks like {@code maven { uri("foo") }} where each occurrence should remain distinct.
     *
     * @return {@code true} if this block should merge with others of the same name (default)
     */
    default boolean shouldMerge() {
        return true;
    }

    /**
     * Extract this block from the given content.
     * <p>
     *
     * @param content the content to search within
     * @return extraction result containing the block content and positions, or empty if not found
     */
    Optional<ExtractionResult> extract(String content);

    /**
     * Result of extracting a block from content.
     *
     * @param blockContent the content inside the block (between braces, after =, etc.)
     * @param startPos the start position of the entire block in the original content
     * @param endPos the end position of the entire block in the original content
     */
    record ExtractionResult(String blockContent, int startPos, int endPos) {}
}
