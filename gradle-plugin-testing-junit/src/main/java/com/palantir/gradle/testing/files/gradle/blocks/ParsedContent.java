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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The result of parsing a Gradle file into structured blocks and unstructured content.
 * <p>
 * Blocks are indexed by name for fast lookup. Content that doesn't match any block pattern
 * is preserved as unstructured content. Provides utilities for parsing, rendering, navigation,
 * and updates.
 */
public record ParsedContent(Map<String, Block> blocks, String unstructuredContent) {

    /**
     * Immutable state during parsing - tracks remaining unparsed content and accumulated blocks.
     */
    public record ParseState(String remaining, Map<String, Block> parsedBlocks) {}

    /**
     * Extract blocks from content using the provided templates and ordering.
     * @param content the text to parse
     * @param blockOrder the order to search for and extract blocks
     * @param blockTemplates block definitions used for pattern matching and parsing
     * @param includeTemplatesInResult if true, start with all templates (preserves empty blocks for navigation)
     * @return parse state with extracted blocks and remaining content
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
                        (state, blockName) -> {
                            Block template = blockTemplates.get(blockName);
                            Matcher matcher = template.pattern().matcher(state.remaining());

                            if (matcher.find()) {
                                state.parsedBlocks().put(blockName, template.parse(matcher.group(1)));
                                String newRemaining = state.remaining().substring(0, matcher.start())
                                        + state.remaining().substring(matcher.end());
                                return new ParseState(newRemaining, state.parsedBlocks());
                            }
                            return state;
                        },
                        (first, second) -> first);
    }

    /**
     * Render blocks to text in the specified order.
     * @param blockOrder the order to render blocks
     * @param blocks the blocks to render
     * @param includeBlockWrapper if true, wrap each block with "name { ... }"
     * @return rendered text with blocks joined by newlines
     */
    public static String renderBlocks(
            List<String> blockOrder, Map<String, Block> blocks, boolean includeBlockWrapper) {
        return blockOrder.stream()
                .map(blocks::get)
                .flatMap(block -> Optional.ofNullable(block).stream())
                .filter(block -> !block.renderContent().isEmpty())
                .map(block -> includeBlockWrapper
                        ? block.name() + " {\n" + indent(block.renderContent()) + "\n}"
                        : block.renderContent())
                .collect(Collectors.joining("\n"));
    }

    /**
     * Add 4-space indentation to each non-empty line.
     */
    public static String indent(String content) {
        return content.lines()
                .map(line -> line.isEmpty() ? line : "    " + line)
                .collect(Collectors.joining("\n"));
    }

    /**
     * Join non-empty content strings with a newline.
     */
    public static String combineUnstructured(String first, String second) {
        return Stream.of(first, second).filter(s -> !s.isEmpty()).collect(Collectors.joining("\n"));
    }

    /**
     * Combine this ParsedContent with another, merging blocks and unstructured content.
     * @param other the ParsedContent to merge
     * @return a new ParsedContent with merged blocks and combined unstructured content
     */
    public ParsedContent merge(ParsedContent other) {
        Map<String, Block> mergedBlocks = Stream.concat(blocks.entrySet().stream(), other.blocks.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Block::merge));

        String mergedUnstructured = Stream.of(unstructuredContent, other.unstructuredContent)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("\n"));

        return new ParsedContent(mergedBlocks, mergedUnstructured);
    }

    /**
     * Navigate to a block using a path of block names.
     * @param templates block definitions for looking up missing blocks
     * @param path array of block names (e.g., ["buildscript", "repositories"])
     * @return the block at the end of the path
     * @throws IllegalStateException if path is invalid or blocks not found
     */
    public Block getBlockAt(Map<String, Block> templates, String... path) {
        if (path.length == 0) {
            throw new IllegalArgumentException("Path cannot be empty");
        }

        Block current = Optional.ofNullable(blocks.get(path[0]))
                .or(() -> Optional.ofNullable(templates.get(path[0])))
                .orElseThrow(() -> new IllegalStateException("Block not found: " + path[0]));

        for (int i = 1; i < path.length; i++) {
            String childName = path[i];
            current = current.getChild(childName)
                    .orElseThrow(() -> new IllegalStateException("Child block not found: " + childName));
        }

        return current;
    }

    /**
     * Create a new ParsedContent with an updated block at the specified path.
     * @param templates block definitions for looking up structure
     * @param path array of block names identifying the target block
     * @param updatedBlock the new block to place at the path
     * @return a new ParsedContent with the block updated
     */
    public ParsedContent withBlockAt(Map<String, Block> templates, String[] path, Block updatedBlock) {
        if (path.length == 0) {
            throw new IllegalArgumentException("Path cannot be empty");
        }

        if (path.length == 1) {
            Map<String, Block> updatedBlocks = new HashMap<>(blocks);
            updatedBlocks.put(path[0], updatedBlock);
            return new ParsedContent(updatedBlocks, unstructuredContent);
        }

        // Recursive update for nested blocks
        String topBlockName = path[0];
        Block topBlock = Optional.ofNullable(blocks.get(topBlockName))
                .or(() -> Optional.ofNullable(templates.get(topBlockName)))
                .orElseThrow(() -> new IllegalStateException("Block not found: " + topBlockName));

        Block updatedTopBlock = updateBlockRecursive(topBlock, path, 1, updatedBlock);
        Map<String, Block> updatedBlocks = new HashMap<>(blocks);
        updatedBlocks.put(topBlockName, updatedTopBlock);
        return new ParsedContent(updatedBlocks, unstructuredContent);
    }

    private Block updateBlockRecursive(Block parent, String[] path, int depth, Block updatedBlock) {
        String childName = path[depth];

        if (depth == path.length - 1) {
            // We're at the parent of the target block - update it
            return parent.withChild(childName, updatedBlock);
        }

        // Need to go deeper
        Block child = parent.getChild(childName)
                .orElseThrow(() -> new IllegalStateException("Child block not found: " + childName));

        Block updatedChild = updateBlockRecursive(child, path, depth + 1, updatedBlock);
        return parent.withChild(childName, updatedChild);
    }
}
