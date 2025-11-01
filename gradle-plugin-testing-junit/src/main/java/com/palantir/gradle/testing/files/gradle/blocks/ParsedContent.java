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
 * Result of parsing a Gradle file.
 * Contains structured blocks and unstructured content that doesn't match any block.
 * Also provides shared parsing and rendering utilities.
 */
public record ParsedContent(Map<String, Block> blocks, String unstructuredContent) {

    /**
     * Parse state tracking remaining content and parsed blocks.
     */
    public record ParseState(String remaining, Map<String, Block> parsedBlocks) {}

    /**
     * Parse content by extracting blocks in order.
     * For NestedClosureBlock: pass includeTemplatesInResult=true to preserve empty blocks.
     * For StructuredGradleFile: pass includeTemplatesInResult=false for top-level parsing.
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
     * Render blocks in order, wrapping each in "name { content }".
     */
    public static String renderBlocks(
            List<String> blockOrder, Map<String, Block> blocks, boolean includeBlockWrapper) {
        return blockOrder.stream()
                .map(blocks::get)
                .flatMap(block -> Optional.ofNullable(block).stream())
                .filter(block -> !block.render().isEmpty())
                .map(block -> includeBlockWrapper
                        ? block.name() + " {\n" + indent(block.render()) + "\n}"
                        : block.render())
                .collect(Collectors.joining("\n"));
    }

    /**
     * Indent each line with 4 spaces.
     */
    public static String indent(String content) {
        return content.lines()
                .map(line -> line.isEmpty() ? line : "    " + line)
                .collect(Collectors.joining("\n"));
    }

    /**
     * Combine unstructured content from two sources.
     */
    public static String combineUnstructured(String first, String second) {
        return Stream.of(first, second).filter(s -> !s.isEmpty()).collect(Collectors.joining("\n"));
    }

    /**
     * Merge another ParsedContent into this one.
     * Blocks with the same name are merged using Block.merge().
     * Unstructured content is combined.
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
     * Navigate to a block at the given path using Block templates.
     * For example, path ["buildscript", "repositories"] navigates to the repositories child
     * of the buildscript block.
     */
    public Block getBlockAt(Map<String, Block> templates, String... path) {
        if (path.length == 0) {
            throw new IllegalArgumentException("Path cannot be empty");
        }

        Block initial = Optional.ofNullable(blocks.get(path[0]))
                .or(() -> Optional.ofNullable(templates.get(path[0])))
                .orElseThrow(() -> new IllegalStateException("Block not found: " + path[0]));

        if (path.length == 1) {
            return initial;
        }

        return Stream.iterate(1, i -> i < path.length, i -> i + 1)
                .reduce(
                        initial,
                        (current, i) -> {
                            if (!(current instanceof NestedClosureBlock nested)) {
                                throw new IllegalStateException(
                                        "Cannot navigate to " + path[i] + " - parent is not nested");
                            }
                            return Optional.ofNullable(nested.children().get(path[i]))
                                    .orElseThrow(() ->
                                            new IllegalStateException("Child block not found: " + path[i]));
                        },
                        (a, b) -> b);
    }

    /**
     * Update a block at the given path, returning a new ParsedContent with the updated block.
     */
    public ParsedContent withBlockAt(Map<String, Block> templates, String[] path, Block updatedBlock) {
        if (path.length == 0) {
            throw new IllegalArgumentException("Path cannot be empty");
        }

        if (path.length == 1) {
            // Top-level block
            Map<String, Block> updatedBlocks = new HashMap<>(blocks);
            updatedBlocks.put(path[0], updatedBlock);
            return new ParsedContent(updatedBlocks, unstructuredContent);
        }

        // Nested block - need to update recursively
        String topBlockName = path[0];
        Block topBlock = Optional.ofNullable(blocks.get(topBlockName))
                .or(() -> Optional.ofNullable(templates.get(topBlockName)))
                .orElseThrow(() -> new IllegalStateException("Block not found in template: " + topBlockName));

        if (!(topBlock instanceof NestedClosureBlock nested)) {
            throw new IllegalStateException("Cannot update nested block - parent is not nested: " + topBlockName);
        }

        Block updatedTopBlock = updateNestedBlock(nested, path, 1, updatedBlock);
        Map<String, Block> updatedBlocks = new HashMap<>(blocks);
        updatedBlocks.put(topBlockName, updatedTopBlock);
        return new ParsedContent(updatedBlocks, unstructuredContent);
    }

    private Block updateNestedBlock(NestedClosureBlock parent, String[] path, int depth, Block updatedBlock) {
        if (depth == path.length - 1) {
            // We're at the parent of the target block
            String childName = path[depth];
            Map<String, Block> updatedChildren = new HashMap<>(parent.children());
            updatedChildren.put(childName, updatedBlock);
            return new NestedClosureBlock(
                    parent.name(), parent.childOrder(), updatedChildren, parent.unstructuredContent());
        }

        // Need to go deeper
        String childName = path[depth];
        NestedClosureBlock nestedChild = Optional.ofNullable(parent.children().get(childName))
                .filter(child -> child instanceof NestedClosureBlock)
                .map(child -> (NestedClosureBlock) child)
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot navigate deeper - child not found or not nested: " + childName));

        Block updatedChild = updateNestedBlock(nestedChild, path, depth + 1, updatedBlock);
        Map<String, Block> updatedChildren = new HashMap<>(parent.children());
        updatedChildren.put(childName, updatedChild);
        return new NestedClosureBlock(
                parent.name(), parent.childOrder(), updatedChildren, parent.unstructuredContent());
    }
}
