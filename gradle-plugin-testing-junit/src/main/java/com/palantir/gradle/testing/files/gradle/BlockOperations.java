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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Unified utility for all block operations: parsing, merging, and list manipulation.
 * <p>
 * Consolidates BlockHelpers, BlockMerger, and BlockParser into a single cohesive API.
 * All methods use streams and Optional instead of null checks for cleaner code.
 */
final class BlockOperations {
    private BlockOperations() {}

    // ========== List Operations ==========

    /**
     * Get the first block from a list, wrapped in Optional.
     */
    static Optional<Block> firstBlock(List<Block> blocks) {
        return Optional.ofNullable(blocks).filter(list -> !list.isEmpty()).map(list -> list.get(0));
    }

    /**
     * Update a block at a specific index in a list, creating new list if needed.
     */
    static List<Block> updateBlockAt(List<Block> blocks, int index, Block newBlock) {
        return Optional.ofNullable(blocks)
                .filter(list -> !list.isEmpty() && index < list.size())
                .map(list -> {
                    List<Block> updated = new ArrayList<>(list);
                    updated.set(index, newBlock);
                    return updated;
                })
                .orElseGet(() -> List.of(newBlock));
    }

    /**
     * Merge a new block into an existing list.
     * Tries to merge with first matching block, otherwise appends.
     */
    static List<Block> mergeIntoList(List<Block> existing, Block newBlock) {
        return Optional.ofNullable(existing)
                .filter(list -> !list.isEmpty())
                .flatMap(list -> findMergeTarget(list, newBlock))
                .orElseGet(() -> appendToList(existing, newBlock));
    }

    private static Optional<List<Block>> findMergeTarget(List<Block> list, Block newBlock) {
        for (int i = 0; i < list.size(); i++) {
            List<Block> merged = list.get(i).merge(newBlock);
            if (merged.size() == 1) {
                return Optional.of(updateBlockAt(list, i, merged.get(0)));
            }
        }
        return Optional.empty();
    }

    private static List<Block> appendToList(List<Block> existing, Block newBlock) {
        List<Block> result = new ArrayList<>(Optional.ofNullable(existing).orElseGet(List::of));
        result.add(newBlock);
        return result;
    }

    // ========== Merging Operations ==========

    /**
     * Merge two block list maps together.
     * Each block list is merged by name, with blocks merged or appended as needed.
     */
    static Map<String, List<Block>> mergeBlockLists(Map<String, List<Block>> first, Map<String, List<Block>> second) {
        Map<String, List<Block>> result = new LinkedHashMap<>(first);

        second.forEach((name, newBlocks) -> result.merge(name, newBlocks, BlockOperations::mergeBlockListsForName));

        return result;
    }

    /**
     * Merge incoming blocks into existing list.
     * Filters out empty blocks and attempts to merge each one.
     */
    private static List<Block> mergeBlockListsForName(List<Block> existing, List<Block> incoming) {
        List<Block> result = Optional.ofNullable(existing).map(ArrayList::new).orElseGet(ArrayList::new);

        incoming.stream()
                .filter(block -> !block.renderContent().isEmpty())
                .forEach(newBlock -> mergeBlockIntoListInPlace(result, newBlock));

        return result;
    }

    /**
     * Merge a block into existing list in-place.
     * Tries to merge with first non-empty block. If successful, replaces it; otherwise appends.
     */
    private static void mergeBlockIntoListInPlace(List<Block> existing, Block newBlock) {
        boolean merged = existing.stream()
                .filter(block -> !block.renderContent().isEmpty())
                .findFirst()
                .map(target -> {
                    int index = existing.indexOf(target);
                    List<Block> mergeResult = target.merge(newBlock);
                    if (mergeResult.size() == 1) {
                        existing.set(index, mergeResult.get(0));
                        return true;
                    }
                    return false;
                })
                .orElse(false);

        if (!merged) {
            existing.add(newBlock);
        }
    }

    // ========== Parsing Operations ==========

    /**
     * Extract and parse all blocks from content according to block order and templates.
     *
     * @param content text to parse
     * @param blockOrder order to search for blocks
     * @param blockTemplates block definitions for pattern matching
     * @param includeTemplatesInResult if true, initialize result with empty template blocks
     * @return ParseResult with extracted blocks and remaining content
     */
    static ParseResult parseBlocks(
            String content,
            List<String> blockOrder,
            Map<String, Block> blockTemplates,
            boolean includeTemplatesInResult) {
        Map<String, List<Block>> initialBlocks = new HashMap<>();
        if (includeTemplatesInResult) {
            blockTemplates.forEach((name, template) -> initialBlocks.put(name, new ArrayList<>(List.of(template))));
        }

        return blockOrder.stream()
                .flatMap(
                        blockName -> Optional.ofNullable(blockTemplates.get(blockName))
                                .map(template -> Map.entry(blockName, template))
                                .stream())
                .reduce(
                        new ParseResult(initialBlocks, content),
                        (result, entry) -> extractAllOccurrences(result, entry.getKey(), entry.getValue()),
                        (first, second) -> first);
    }

    /**
     * Parse nested block content using child templates.
     */
    static ParseResult parseNestedBlocks(String content, List<String> childOrder, Map<String, Block> childTemplates) {
        return parseBlocks(content, childOrder, childTemplates, true);
    }

    /**
     * Extract all occurrences of a block from the parse result.
     * Repeatedly extracts until no more found, collecting blocks.
     */
    private static ParseResult extractAllOccurrences(ParseResult result, String blockName, Block template) {
        List<Block> extractedBlocks = new ArrayList<>();
        String remaining = result.remaining();

        Optional<Block.ExtractionResult> extraction = template.extract(remaining);
        while (extraction.isPresent()) {
            Block.ExtractionResult extr = extraction.get();
            extractedBlocks.add(template.parse(extr.blockContent()));
            remaining = removeExtractedBlock(remaining, extr);
            extraction = template.extract(remaining);
        }

        return extractedBlocks.isEmpty()
                ? result
                : createResultWithBlocks(result, blockName, extractedBlocks, remaining);
    }

    /**
     * Remove an extracted block from content.
     */
    private static String removeExtractedBlock(String content, Block.ExtractionResult extraction) {
        return content.substring(0, extraction.startPos()) + content.substring(extraction.endPos());
    }

    /**
     * Create a new ParseResult with blocks added/merged.
     * Merges multiple extracted blocks together by repeatedly calling merge().
     */
    private static ParseResult createResultWithBlocks(
            ParseResult originalResult, String blockName, List<Block> extractedBlocks, String remaining) {
        // Merge all extracted blocks together
        List<Block> blocksToStore = extractedBlocks.stream()
                .skip(1)
                .reduce(List.of(extractedBlocks.get(0)), BlockOperations::mergeIntoList, (first, second) -> second);

        Map<String, List<Block>> updatedBlocks = new HashMap<>(originalResult.blocks());
        updatedBlocks.put(blockName, blocksToStore);
        return new ParseResult(updatedBlocks, remaining);
    }
}
