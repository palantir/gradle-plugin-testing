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

import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * A structural unit in a Gradle file that can be parsed, rendered, merged, and edited.
 * <p>
 * Blocks are self-contained - each block knows its own pattern, parsing rules, and rendering logic.
 * This enables uniform treatment of different block types (closure, nested, property) without
 * requiring external configuration or templates.
 * <p>
 * Examples: plugins { }, buildscript { repositories { } }, version = '1.0'
 */
public sealed interface Block permits ClosureBlock, NestedClosureBlock, PropertyBlock {
    /**
     * Parse raw content into a Block instance.
     * @param content the text that appears inside the block (without wrapper syntax)
     * @return a new Block containing the parsed content
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
     * @param other the block to merge
     * @return a new Block containing both this block's content and the other's content
     */
    Block merge(Block other);

    /**
     * Transform this block's content using an editor function.
     * @param editor function that receives current content and returns new content
     * @return a new Block with the transformed content
     */
    Block edit(Function<String, String> editor);

    /**
     * The regex pattern used to identify and extract this block from parent content.
     * @return pattern with capture group 1 containing the block's inner content
     */
    Pattern pattern();

    /**
     * The identifier for this block.
     * @return block name (e.g., "plugins", "repositories", "buildscript")
     */
    String name();
}
