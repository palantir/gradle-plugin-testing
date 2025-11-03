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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Handles parsing of block content using templates and extraction logic.
 * <p>
 * Separates parsing concerns from data representation, providing a clean
 * functional interface using streams and Optional instead of imperative loops.
 */
final class BlockParser {
    private BlockParser() {}

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
     * Extract all occurrences of a block from the parse result using streams instead of while loops.
     */
    private static ParseResult extractAllOccurrences(ParseResult result, String blockName, Block template) {
        // Generate stream of extractions until no more found
        List<Block> extractedBlocks = Stream.iterate(
                        template.extract(result.remaining()),
                        Optional::isPresent,
                        prev -> template.extract(removeExtractedBlock(result.remaining(), prev.orElseThrow())))
                .map(Optional::orElseThrow)
                .map(extraction -> template.parse(extraction.blockContent()))
                .toList();

        return Optional.of(extractedBlocks)
                .filter(blocks -> !blocks.isEmpty())
                .map(blocks -> createResultWithBlocks(result, blockName, template, blocks))
                .orElse(result);
    }

    /**
     * Remove an extracted block from content.
     */
    private static String removeExtractedBlock(String content, Block.ExtractionResult extraction) {
        return content.substring(0, extraction.startPos()) + content.substring(extraction.endPos());
    }

    /**
     * Create a new ParseResult with blocks added/merged according to shouldMerge flag.
     */
    private static ParseResult createResultWithBlocks(
            ParseResult originalResult, String blockName, Block template, List<Block> extractedBlocks) {
        // Remove all extracted blocks from content
        String remainingContent = extractedBlocks.stream()
                .reduce(
                        originalResult.remaining(),
                        (content, block) -> template.extract(content)
                                .map(extraction -> removeExtractedBlock(content, extraction))
                                .orElse(content),
                        (first, second) -> second);

        List<Block> blocksToStore = template.shouldMerge() && extractedBlocks.size() > 1
                ? List.of(extractedBlocks.stream().reduce(Block::merge).orElseThrow())
                : extractedBlocks;

        Map<String, List<Block>> updatedBlocks = new HashMap<>(originalResult.blocks());
        updatedBlocks.put(blockName, blocksToStore);
        return new ParseResult(updatedBlocks, remainingContent);
    }

    /**
     * Parse nested block content using child templates.
     *
     * @param content the content inside a closure block
     * @param childOrder order of child blocks
     * @param childTemplates templates for parsing children
     * @return ParseResult with parsed children and remaining unstructured content
     */
    static ParseResult parseNestedBlocks(String content, List<String> childOrder, Map<String, Block> childTemplates) {
        return parseBlocks(content, childOrder, childTemplates, true);
    }
}
