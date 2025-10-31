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

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable state container for Gradle file content.
 * Stores named blocks (e.g., plugins, repositories) and unstructured content.
 */
public record GradleFileState(ImmutableMap<String, String> namedBlocks, String unstructuredContent) {
    public static GradleFileState empty() {
        return new GradleFileState(ImmutableMap.of(), "");
    }

    public String getBlock(String blockName) {
        return namedBlocks.getOrDefault(blockName, "");
    }

    public GradleFileState withBlock(String blockName, String content) {
        if (content.trim().isEmpty()) {
            return new GradleFileState(
                    namedBlocks.entrySet().stream()
                            .filter(e -> !e.getKey().equals(blockName))
                            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)),
                    unstructuredContent);
        }

        return new GradleFileState(
                ImmutableMap.<String, String>builder()
                        .putAll(namedBlocks.entrySet().stream()
                                .filter(e -> !e.getKey().equals(blockName))
                                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)))
                        .put(blockName, content.trim())
                        .buildOrThrow(),
                unstructuredContent);
    }

    GradleFileState withUnstructuredContent(String content) {
        return new GradleFileState(namedBlocks, content);
    }

    Optional<String> getNamedBlock(String blockName) {
        return Optional.ofNullable(namedBlocks.get(blockName)).filter(s -> !s.isEmpty());
    }

    boolean hasBlock(String blockName) {
        return namedBlocks.containsKey(blockName) && !namedBlocks.get(blockName).isEmpty();
    }
}
