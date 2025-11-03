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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Utility for merging block list maps.
 */
final class BlockMerger {
    private BlockMerger() {}

    /**
     * Merges two block list maps together.
     * If a block has shouldMerge=true, it merges with the first non-empty existing block.
     * Otherwise, it's added as a separate block in the list.
     */
    static Map<String, List<Block>> mergeBlockLists(Map<String, List<Block>> first, Map<String, List<Block>> second) {
        Map<String, List<Block>> result = new LinkedHashMap<>(first);
        result.replaceAll((name, blocks) -> new ArrayList<>(blocks));

        second.forEach((name, newBlocks) -> result.put(name, mergeBlockListsForName(result.get(name), newBlocks)));

        return result;
    }

    /**
     * Merge new blocks into existing block list for a given name.
     */
    private static List<Block> mergeBlockListsForName(List<Block> existing, List<Block> newBlocks) {
        return java.util.Optional.ofNullable(existing)
                .map(existingList -> {
                    List<Block> mutableList = new ArrayList<>(existingList);
                    newBlocks.stream()
                            .filter(block -> !block.renderContent().isEmpty())
                            .forEach(newBlock -> mergeOrAddBlock(mutableList, newBlock));
                    return mutableList;
                })
                .orElseGet(() -> new ArrayList<>(newBlocks));
    }

    /**
     * Merge a block with the first non-empty block in the list, or add it if shouldMerge=false or no merge target
     * found.
     */
    private static void mergeOrAddBlock(List<Block> existing, Block newBlock) {
        if (!newBlock.shouldMerge()) {
            existing.add(newBlock);
            return;
        }

        // Find first non-empty block and merge with it
        IntStream.range(0, existing.size())
                .filter(i -> !existing.get(i).renderContent().isEmpty())
                .findFirst()
                .ifPresentOrElse(i -> existing.set(i, existing.get(i).merge(newBlock)), () -> existing.add(newBlock));
    }
}
