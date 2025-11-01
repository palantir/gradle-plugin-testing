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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parsed Gradle file containing structured blocks and unstructured content.
 * <p>
 * Blocks are indexed by name. Unmatched content is preserved as {@code unstructuredContent}.
 * Provides utilities for parsing, rendering, navigation, and immutable updates.
 * <p>
 * This class acts as an intermediate representation between raw file text and structured
 * {@link Block} objects, enabling manipulation of Gradle files while preserving structure
 * and formatting.
 *
 * @see Block
 * @see ClosureBlock
 * @see PropertyBlock
 * @see BlockEditor
 */
public record ParsedContent(Map<String, Block> blocks, String unstructuredContent) {

    /**
     * Intermediate parsing state - remaining content and accumulated blocks.
     * Used during the parsing process to track progress through the file.
     */
    public record ParseState(String remaining, Map<String, Block> parsedBlocks) {}

    /**
     * Extract blocks from content using templates and ordering.
     *
     * @param content text to parse
     * @param blockOrder order to search for blocks
     * @param blockTemplates block definitions for pattern matching
     * @param includeTemplatesInResult if {@code true}, preserve empty blocks (for nested navigation)
     * @return {@link ParseState} with extracted blocks and remaining content
     */
    public static ParseState parseBlocks(
            String content,
            List<String> blockOrder,
            Map<String, Block> blockTemplates,
            boolean includeTemplatesInResult) {
        Map<String, Block> initialBlocks = includeTemplatesInResult ? new HashMap<>(blockTemplates) : new HashMap<>();

        return blockOrder.stream()
                .filter(blockTemplates::containsKey)
                .reduce(
                        new ParseState(content, initialBlocks),
                        (state, blockName) -> extractBlock(state, blockName, blockTemplates.get(blockName)),
                        (first, second) -> first);
    }

    /**
     * Extract a single block from parse state.
     */
    private static ParseState extractBlock(ParseState state, String blockName, Block template) {
        Matcher matcher = template.pattern().matcher(state.remaining());

        if (!matcher.find()) {
            return state;
        }

        state.parsedBlocks().put(blockName, template.parse(matcher.group(1)));
        String newRemaining = state.remaining().substring(0, matcher.start())
                + state.remaining().substring(matcher.end());
        return new ParseState(newRemaining, state.parsedBlocks());
    }

    /**
     * Render blocks to text in the specified order.
     *
     * @param blockOrder the order to render blocks
     * @param blocks the blocks to render
     * @param includeBlockWrapper if {@code true}, wrap each block with {@code "name { ... }"}
     * @return rendered text with blocks joined by newlines
     */
    public static String renderBlocks(List<String> blockOrder, Map<String, Block> blocks, boolean includeBlockWrapper) {
        return nonEmptyBlocksInOrder(blockOrder, blocks)
                .map(block -> formatBlock(block, includeBlockWrapper))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Format a block with optional wrapper.
     */
    private static String formatBlock(Block block, boolean includeBlockWrapper) {
        return includeBlockWrapper
                ? block.name() + " {\n" + indent(block.renderContent()) + "\n}"
                : block.renderContent();
    }

    /**
     * Stream of non-empty rendered blocks in order.
     */
    private static Stream<Block> nonEmptyBlocksInOrder(List<String> blockOrder, Map<String, Block> blocks) {
        return blockOrder.stream().map(blocks::get).filter(Objects::nonNull).filter(block -> !block.renderContent()
                .isEmpty());
    }

    /**
     * Indent each non-empty line with 4 spaces.
     */
    public static String indent(String content) {
        return content.lines()
                .map(line -> line.isEmpty() ? line : "    " + line)
                .collect(Collectors.joining("\n"));
    }

    /**
     * Join non-empty strings with newline.
     */
    public static String combineUnstructured(String first, String second) {
        return joinNonEmpty("\n", first, second);
    }

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
    public static ParsedContent parseContent(
            String content, List<String> blockOrder, Map<String, Block> blockTemplates) {
        if (content == null || content.trim().isEmpty()) {
            return new ParsedContent(Map.of(), "");
        }

        ParseState result = parseBlocks(content, blockOrder, blockTemplates, false);
        String normalizedRemaining = normalizeWhitespace(result.remaining());
        return new ParsedContent(result.parsedBlocks(), normalizedRemaining);
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
    public static String renderContent(ParsedContent content, List<String> blockOrder) {
        String blocks = nonEmptyBlocksInOrder(blockOrder, content.blocks())
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
     * Blocks with the same name are merged using {@link Block#merge(Block)}.
     *
     * @param other content to merge
     * @return new {@link ParsedContent} with merged blocks and combined unstructured content
     */
    public ParsedContent merge(ParsedContent other) {
        Map<String, Block> mergedBlocks = mergeBlockMaps(blocks, other.blocks);
        String mergedUnstructured = joinNonEmpty("\n", unstructuredContent, other.unstructuredContent);
        return new ParsedContent(mergedBlocks, mergedUnstructured);
    }

    /**
     * Merge two block maps, combining values for duplicate keys.
     */
    private static Map<String, Block> mergeBlockMaps(Map<String, Block> first, Map<String, Block> second) {
        return Stream.concat(first.entrySet().stream(), second.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Block::merge));
    }

    /**
     * Navigate to a block by path.
     * Supports nested navigation through closure blocks.
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
                .reduce(getBlockOrTemplate(path[0], templates), ParsedContent::getChildBlock, (a, b) -> b);
    }

    /**
     * Update a block at a path.
     * Returns a new immutable {@link ParsedContent} with the specified block updated.
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

        Block topBlock = getBlockOrTemplate(path[0], templates);
        Block updatedTopBlock = updateBlockRecursive(topBlock, path, 1, updatedBlock);
        return withTopLevelBlock(path[0], updatedTopBlock);
    }

    /**
     * Create new ParsedContent with updated top-level block.
     */
    private ParsedContent withTopLevelBlock(String blockName, Block updatedBlock) {
        Map<String, Block> updatedBlocks = new HashMap<>(blocks);
        updatedBlocks.put(blockName, updatedBlock);
        return new ParsedContent(updatedBlocks, unstructuredContent);
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
     * Get block with fallback to template.
     */
    private Block getBlockOrTemplate(String blockName, Map<String, Block> templates) {
        return Optional.ofNullable(blocks.get(blockName))
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
