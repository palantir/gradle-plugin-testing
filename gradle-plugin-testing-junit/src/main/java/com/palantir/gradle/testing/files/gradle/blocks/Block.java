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
 * Represents a block in a Gradle file (e.g., plugins, repositories, buildscript).
 * Blocks are recursive, self-contained units that know how to parse, render, merge, and edit themselves.
 */
public sealed interface Block permits ClosureBlock, NestedClosureBlock, PropertyBlock {
    /**
     * Parse content into this block. The content is what appears INSIDE the braces.
     * For example, for "plugins { id 'java' }", the content is "id 'java'".
     */
    Block parse(String content);

    /**
     * Render this block's content (what goes INSIDE the braces).
     * Does not include the block name or braces themselves.
     */
    String render();

    /**
     * Merge another block's content into this block.
     * For simple blocks, this typically appends content.
     * For nested blocks, this recursively merges children.
     */
    Block merge(Block other);

    /**
     * Edit this block's content locally.
     * The editor receives the current rendered content and returns new content.
     * This is then parsed back into the block structure.
     */
    Block edit(Function<String, String> editor);

    /**
     * Pattern for matching this block in parent content.
     * The pattern should capture the content inside braces in group 1.
     */
    Pattern pattern();

    /**
     * The name of this block (e.g., "plugins", "repositories").
     */
    String name();
}
