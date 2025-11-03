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
import java.util.regex.Pattern;

/**
 * A structural unit in a Gradle file that can be parsed, rendered, merged, and edited.
 * <p>
 * Blocks are self-contained - each block knows its own pattern, parsing rules, and rendering logic.
 * This enables uniform treatment of different block types ({@link ClosureBlock}, {@link PropertyBlock}) without
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
 * @see PropertyBlock
 * @see StatementBlock
 * @see ParsedContent
 */
sealed interface Block permits ClosureBlock, PropertyBlock, StatementBlock {
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
    Block edit(Function<String, String> editor);

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
     * The regex pattern used to identify and extract this block from parent content.
     * @return pattern with capture group 1 containing the block's inner content
     */
    Pattern pattern();

    /**
     * The identifier for this block.
     *
     * @return block name (e.g., {@code "plugins"}, {@code "repositories"}, {@code "buildscript"})
     */
    String name();
}
