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

        second.forEach((name, newBlocks) -> {
            List<Block> existing = result.get(name);
            if (existing == null) {
                result.put(name, new ArrayList<>(newBlocks));
                return;
            }

            for (Block newBlock : newBlocks) {
                if (newBlock.renderContent().isEmpty()) {
                    continue; // Skip empty template blocks
                }

                if (!newBlock.shouldMerge()) {
                    existing.add(newBlock);
                    continue;
                }

                // Find first non-empty block to merge with
                boolean merged = false;
                for (int i = 0; i < existing.size(); i++) {
                    if (!existing.get(i).renderContent().isEmpty()) {
                        existing.set(i, existing.get(i).merge(newBlock));
                        merged = true;
                        break;
                    }
                }
                if (!merged) {
                    existing.add(newBlock);
                }
            }
        });

        return result;
    }
}
