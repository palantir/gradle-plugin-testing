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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parsed Gradle file containing structured blocks and unstructured content.
 * <p>
 * Blocks are indexed by name, with each name potentially having multiple blocks (stored in a list).
 * Unmatched content is preserved as {@code unstructuredContent}.
 * Provides utilities for parsing, rendering, navigation, and immutable updates.
 * <p>
 * This class acts as an intermediate representation between raw file text and structured
 * {@link Block} objects, enabling manipulation of Gradle files while preserving structure
 * and formatting.
 *
 * @see Block
 * @see ClosureBlock
 * @see BlockEditor
 */
record ParsedContent(Map<String, List<Block>> blocks, String unstructuredContent) {

    /**
     * Join non-empty strings with specified delimiter.
     */
    private static String joinNonEmpty(String delimiter, String... strings) {
        return Stream.of(strings).filter(s -> !s.isEmpty()).collect(Collectors.joining(delimiter));
    }

    /**
     * Parse content string into structured blocks and unstructured remainder.
     * This is the main entry point for parsing Gradle file content.
     *
     * @param content the text to parse
     * @param blockOrder order to search for blocks
     * @param blockTemplates block definitions for pattern matching
     * @return {@link ParsedContent} with extracted blocks and remaining unstructured text
     */
    static ParsedContent parseContent(String content, List<String> blockOrder, Map<String, Block> blockTemplates) {
        return Optional.ofNullable(content)
                .filter(c -> !c.trim().isEmpty())
                .map(c -> {
                    ParseResult result = BlockParser.parseBlocks(c, blockOrder, blockTemplates, false);
                    String normalizedRemaining = normalizeWhitespace(result.remaining());
                    return new ParsedContent(result.blocks(), normalizedRemaining);
                })
                .orElseGet(() -> new ParsedContent(Map.of(), ""));
    }

    /**
     * Normalize whitespace: trim and collapse multiple newlines.
     */
    private static String normalizeWhitespace(String content) {
        return content.trim().replaceAll("\n{2,}", "\n");
    }

    /**
     * Render {@link ParsedContent} back to text with proper block ordering and formatting.
     * This is the main entry point for rendering parsed content back to file text.
     *
     * @param content the parsed content to render
     * @param blockOrder order to render blocks
     * @return formatted text with blocks followed by unstructured content
     */
    static String renderContent(ParsedContent content, List<String> blockOrder) {
        String blocks = blockOrder.stream()
                .filter(content.blocks()::containsKey)
                .flatMap(blockName -> content.blocks().get(blockName).stream())
                .filter(block -> !block.renderContent().isEmpty())
                .map(Block::renderBlock)
                .collect(Collectors.joining("\n\n"));

        String result = joinNonEmpty("\n\n", blocks, content.unstructuredContent());
        return ensureTrailingNewline(result);
    }

    /**
     * Ensure string ends with newline if non-empty.
     */
    private static String ensureTrailingNewline(String content) {
        return content.isEmpty() || content.endsWith("\n") ? content : content + "\n";
    }

    /**
     * Merge with another {@link ParsedContent}.
     * Blocks with the same name are merged based on their shouldMerge flag.
     *
     * @param other content to merge
     * @return new {@link ParsedContent} with merged blocks and combined unstructured content
     */
    ParsedContent merge(ParsedContent other) {
        Map<String, List<Block>> mergedBlocks = mergeBlockMaps(blocks, other.blocks);
        String mergedUnstructured = joinNonEmpty("\n", unstructuredContent, other.unstructuredContent);
        return new ParsedContent(mergedBlocks, mergedUnstructured);
    }

    /**
     * Merge two block maps. For blocks with shouldMerge=true, merge them together.
     * For blocks with shouldMerge=false, keep them separate in the list.
     */
    private static Map<String, List<Block>> mergeBlockMaps(
            Map<String, List<Block>> first, Map<String, List<Block>> second) {
        return BlockMerger.mergeBlockLists(first, second);
    }

    /**
     * Navigate to a block by path.
     * Supports nested navigation through closure blocks.
     * Returns the first block if multiple blocks exist for a name.
     *
     * @param templates block definitions for missing blocks
     * @param path block names (e.g., {@code ["buildscript", "repositories"]})
     * @return {@link Block} at path end
     * @throws IllegalStateException if path invalid or block not found
     * @throws IllegalArgumentException if path is empty
     */
    Block getBlockAt(Map<String, Block> templates, String... path) {
        validatePath(path);

        return Arrays.stream(path)
                .skip(1)
                .reduce(getFirstBlockOrTemplate(path[0], templates), ParsedContent::getChildBlock, (a, b) -> b);
    }

    /**
     * Update a block at a path.
     * Returns a new immutable {@link ParsedContent} with the specified block updated.
     * Updates the first block if multiple blocks exist for a name.
     *
     * @param templates block definitions for structure lookup
     * @param path block names identifying target
     * @param updatedBlock new block
     * @return new {@link ParsedContent} with updated block
     * @throws IllegalStateException if block at path not found
     * @throws IllegalArgumentException if path is empty
     */
    ParsedContent withBlockAt(Map<String, Block> templates, String[] path, Block updatedBlock) {
        validatePath(path);

        if (path.length == 1) {
            return withTopLevelBlock(path[0], updatedBlock);
        }

        Block topBlock = getFirstBlockOrTemplate(path[0], templates);
        Block updatedTopBlock = updateBlockRecursive(topBlock, path, 1, updatedBlock);
        return withTopLevelBlock(path[0], updatedTopBlock);
    }

    /**
     * Create new ParsedContent with updated top-level block (first block if multiple exist).
     */
    private ParsedContent withTopLevelBlock(String blockName, Block updatedBlock) {
        Map<String, List<Block>> updatedBlocks = new HashMap<>(blocks);
        updatedBlocks.put(blockName, updateBlockListAtIndex(blocks.get(blockName), 0, updatedBlock));
        return new ParsedContent(updatedBlocks, unstructuredContent);
    }

    /**
     * Update a block list at a specific index, or create new list if null/empty.
     */
    private static List<Block> updateBlockListAtIndex(List<Block> existingList, int index, Block updatedBlock) {
        return Optional.ofNullable(existingList)
                .filter(list -> !list.isEmpty())
                .map(list -> {
                    List<Block> newList = new ArrayList<>(list);
                    newList.set(index, updatedBlock);
                    return newList;
                })
                .orElseGet(() -> new ArrayList<>(List.of(updatedBlock)));
    }

    /**
     * Recursively update nested block.
     */
    private Block updateBlockRecursive(Block parent, String[] path, int depth, Block updatedBlock) {
        String childName = path[depth];

        if (depth == path.length - 1) {
            return parent.withChild(childName, updatedBlock);
        }

        Block child = getChildBlock(parent, childName);
        Block updatedChild = updateBlockRecursive(child, path, depth + 1, updatedBlock);
        return parent.withChild(childName, updatedChild);
    }

    /**
     * Get first block with fallback to template.
     */
    private Block getFirstBlockOrTemplate(String blockName, Map<String, Block> templates) {
        return Optional.ofNullable(blocks.get(blockName))
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .or(() -> Optional.ofNullable(templates.get(blockName)))
                .orElseThrow(() -> new IllegalStateException("Block not found: " + blockName));
    }

    /**
     * Get child block or throw exception.
     */
    private static Block getChildBlock(Block parent, String childName) {
        return parent.getChild(childName)
                .orElseThrow(() -> new IllegalStateException("Child block not found: " + childName));
    }

    /**
     * Validate path is non-empty.
     */
    private static void validatePath(String[] path) {
        if (path.length == 0) {
            throw new IllegalArgumentException("Path cannot be empty");
        }
    }
}
