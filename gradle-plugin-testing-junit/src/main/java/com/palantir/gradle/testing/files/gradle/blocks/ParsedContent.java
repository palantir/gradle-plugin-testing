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
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Result of parsing a Gradle file.
 * Contains structured blocks and unstructured content that doesn't match any block.
 */
public record ParsedContent(Map<String, Block> blocks, String unstructuredContent) {

    /**
     * Merge another ParsedContent into this one.
     * Blocks with the same name are merged using Block.merge().
     * Unstructured content is combined.
     */
    public ParsedContent merge(ParsedContent other) {
        Map<String, Block> mergedBlocks = Stream.concat(blocks.entrySet().stream(), other.blocks.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, incoming) -> existing.merge(incoming)));

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

        Block current = blocks.get(path[0]);
        if (current == null) {
            // Block doesn't exist - get from template
            current = templates.get(path[0]);
            if (current == null) {
                throw new IllegalStateException("Block not found: " + path[0]);
            }
        }

        // Navigate deeper if needed
        for (int i = 1; i < path.length; i++) {
            if (!(current instanceof NestedClosureBlock nested)) {
                throw new IllegalStateException("Cannot navigate to " + path[i] + " - parent is not nested");
            }

            Block child = nested.children().get(path[i]);
            if (child == null) {
                throw new IllegalStateException("Child block not found: " + path[i]);
            }
            current = child;
        }

        return current;
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
        Block topBlock = blocks.get(topBlockName);
        if (topBlock == null) {
            // Block doesn't exist - get from template
            topBlock = templates.get(topBlockName);
            if (topBlock == null) {
                throw new IllegalStateException("Block not found in template: " + topBlockName);
            }
        }

        if (!(topBlock instanceof NestedClosureBlock nested)) {
            throw new IllegalStateException("Cannot update nested block - parent is not nested: " + topBlockName);
        }

        Block updatedTopBlock = updateNestedBlock(nested, path, 1, updatedBlock);
        Map<String, Block> updatedBlocks = new HashMap<>(blocks);
        updatedBlocks.put(topBlockName, updatedTopBlock);
        return new ParsedContent(updatedBlocks, unstructuredContent);
    }

    private Block updateNestedBlock(
            NestedClosureBlock parent, String[] path, int depth, Block updatedBlock) {
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
        Block child = parent.children().get(childName);
        if (child == null) {
            throw new IllegalStateException("Child block not found: " + childName);
        }
        if (!(child instanceof NestedClosureBlock nestedChild)) {
            throw new IllegalStateException("Cannot navigate deeper - child is not nested: " + childName);
        }

        Block updatedChild = updateNestedBlock(nestedChild, path, depth + 1, updatedBlock);
        Map<String, Block> updatedChildren = new HashMap<>(parent.children());
        updatedChildren.put(childName, updatedChild);
        return new NestedClosureBlock(
                parent.name(), parent.childOrder(), updatedChildren, parent.unstructuredContent());
    }
}
