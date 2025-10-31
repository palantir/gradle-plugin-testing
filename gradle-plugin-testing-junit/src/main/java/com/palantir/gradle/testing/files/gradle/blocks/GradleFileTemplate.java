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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Template for parsing and rendering Gradle files with structured blocks.
 * Maintains canonical ordering of blocks and handles nested content.
 */
public interface GradleFileTemplate {
    /**
     * Render state to Gradle file content.
     */
    default String render(GradleFileState state) {
        String sections = Stream.concat(
                        blockNames().stream().map(blockName -> renderBlock(blockName, state)),
                        Stream.of(state.unstructuredContent()))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("\n\n"));

        return sections.isEmpty() || sections.endsWith("\n") ? sections : sections + "\n";
    }

    /**
     * Parse Gradle file content into state.
     */
    default GradleFileState parse(String content) {
        return parseBlocks(content, blockNames());
    }

    /**
     * Get the canonical ordering of named blocks.
     */
    ImmutableList<String> blockNames();

    /**
     * Parse a specific block using the appropriate parsing strategy.
     */
    default GradleFileState parseBlocks(String content, List<String> blockNames) {
        if (content == null || content.trim().isEmpty()) {
            return GradleFileState.empty();
        }

        String remaining = content;
        ImmutableMap.Builder<String, String> blocksBuilder = ImmutableMap.builder();

        for (String blockName : blockNames) {
            Pattern pattern = blockPattern(blockName);
            Matcher matcher = pattern.matcher(remaining);

            if (matcher.find()) {
                blocksBuilder.put(blockName, matcher.group(1).trim());
                remaining = remaining.substring(0, matcher.start()) + remaining.substring(matcher.end());
            }
        }

        // Normalize remaining content: collapse multiple consecutive blank lines to single blank line
        String normalizedRemaining = remaining.trim().replaceAll("\n{2,}", "\n");

        return new GradleFileState(blocksBuilder.buildOrThrow(), normalizedRemaining);
    }

    default Pattern blockPattern(String blockName) {
        return Pattern.compile(
                "^\\s*" + Pattern.quote(blockName) + "\\s*\\{([^{}]*(?:\\{[^{}]*\\}[^{}]*)*)\\}",
                Pattern.MULTILINE | Pattern.DOTALL);
    }

    default String renderBlock(String blockName, GradleFileState state) {
        return state.getNamedBlock(blockName)
                .map(content -> formatBlock(blockName, content))
                .orElse("");
    }

    default String formatBlock(String blockName, String content) {
        String indentedContent = indentContent(content);
        return String.format("%s {\n%s\n}", blockName, indentedContent);
    }

    default String indentContent(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        int[] currentLevel = {1};

        List<String> lines = Splitter.on('\n').splitToList(content);
        return lines.stream()
                .map(line -> {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) {
                        return "";
                    }

                    int lineLevel = currentLevel[0];
                    if (trimmed.startsWith("}")) {
                        lineLevel = Math.max(0, currentLevel[0] - 1);
                    }

                    String indented = "    ".repeat(lineLevel) + trimmed;

                    if (trimmed.endsWith("{") && !trimmed.startsWith("}")) {
                        currentLevel[0]++;
                    } else if (trimmed.equals("}")) {
                        currentLevel[0] = Math.max(1, currentLevel[0] - 1);
                    }

                    return indented;
                })
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("\n"));
    }
}
