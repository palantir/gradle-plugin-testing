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
     * Tries to merge each new block with the first non-empty existing block.
     * If merge() returns a single block, the blocks were merged successfully.
     * If merge() returns two blocks, they couldn't merge and the new one is added separately.
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
     * Try to merge a block with the first non-empty block in the list.
     * If merge returns a single block, replace the existing one.
     * If merge returns two blocks, add the new one.
     */
    private static void mergeOrAddBlock(List<Block> existing, Block newBlock) {
        // Find first non-empty block and try to merge with it
        IntStream.range(0, existing.size())
                .filter(i -> !existing.get(i).renderContent().isEmpty())
                .findFirst()
                .ifPresentOrElse(
                        i -> {
                            List<Block> mergeResult = existing.get(i).merge(newBlock);
                            if (mergeResult.size() == 1) {
                                // Blocks merged successfully
                                existing.set(i, mergeResult.get(0));
                            } else {
                                // Blocks didn't merge, add the new one
                                existing.add(newBlock);
                            }
                        },
                        () -> existing.add(newBlock));
    }

    /**
     * Merge a new block into a list of blocks, returning a new list.
     * Tries to merge with each existing block. If merge succeeds (returns 1 block), replaces at that position.
     * If no merge succeeds, adds the new block to the end.
     */
    static List<Block> mergeBlockIntoList(List<Block> existingBlocks, Block newBlock) {
        for (int i = 0; i < existingBlocks.size(); i++) {
            List<Block> mergeResult = existingBlocks.get(i).merge(newBlock);
            if (mergeResult.size() == 1) {
                // Merge succeeded, replace at this position
                List<Block> result = new ArrayList<>(existingBlocks);
                result.set(i, mergeResult.get(0));
                return result;
            }
        }
        // No merge succeeded, add to end
        List<Block> result = new ArrayList<>(existingBlocks);
        result.add(newBlock);
        return result;
    }
}
