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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Template for build.gradle files.
 * Defines canonical block ordering: buildscript, plugins, allprojects, subprojects,
 * repositories, dependencies, configurations.
 */
public final class BuildGradleTemplate implements Template {

    public static final BuildGradleTemplate INSTANCE = builder()
            .block(NestedClosureBlock.builder("buildscript")
                    .child("repositories")
                    .child("dependencies")
                    .child("plugins")
                    .build())
            .block("plugins")
            .block("allprojects")
            .block("subprojects")
            .block("repositories")
            .block("dependencies")
            .block(NestedClosureBlock.builder("configurations")
                    .child(NestedClosureBlock.builder("all")
                            .child("resolutionStrategy")
                            .build())
                    .build())
            .build();

    private final List<String> blockOrder;
    private final Map<String, Block> blockTemplates;

    private BuildGradleTemplate(List<String> blockOrder, Map<String, Block> blockTemplates) {
        this.blockOrder = blockOrder;
        this.blockTemplates = blockTemplates;
    }

    @Override
    public ParsedContent parse(String fileContent) {
        if (fileContent == null || fileContent.trim().isEmpty()) {
            return new ParsedContent(Map.of(), "");
        }

        String remaining = fileContent;
        Map<String, Block> parsedBlocks = new HashMap<>();

        // Each block parses itself using its pattern
        for (String blockName : blockOrder) {
            Block template = blockTemplates.get(blockName);
            Pattern pattern = template.pattern();
            Matcher matcher = pattern.matcher(remaining);

            if (matcher.find()) {
                String innerContent = matcher.group(1);
                parsedBlocks.put(blockName, template.parse(innerContent));

                // Remove matched content
                remaining = remaining.substring(0, matcher.start()) + remaining.substring(matcher.end());
            }
        }

        return new ParsedContent(parsedBlocks, normalizeRemaining(remaining));
    }

    @Override
    public Map<String, Block> blockTemplates() {
        return blockTemplates;
    }

    @Override
    public String render(ParsedContent content) {
        List<String> parts = new ArrayList<>();

        // Render blocks in canonical order
        for (String blockName : blockOrder) {
            Block block = content.blocks().get(blockName);
            if (block == null) {
                continue;
            }

            String blockContent = block.render();
            if (!blockContent.isEmpty()) {
                parts.add(blockName + " {\n" + indent(blockContent) + "\n}");
            }
        }

        // Add unstructured content at the end
        if (!content.unstructuredContent().isEmpty()) {
            parts.add(content.unstructuredContent());
        }

        String result = String.join("\n\n", parts);
        return result.isEmpty() || result.endsWith("\n") ? result : result + "\n";
    }

    private String normalizeRemaining(String remaining) {
        // Collapse multiple consecutive blank lines to single blank line
        return remaining.trim().replaceAll("\n{2,}", "\n");
    }

    private String indent(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        return content.lines()
                .map(line -> line.isEmpty() ? line : "    " + line)
                .collect(Collectors.joining("\n"));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<String> blockOrder = new ArrayList<>();
        private final Map<String, Block> blocks = new HashMap<>();

        private Builder() {}

        public Builder block(String name) {
            blockOrder.add(name);
            blocks.put(name, ClosureBlock.builder(name).build());
            return this;
        }

        public Builder block(Block block) {
            blockOrder.add(block.name());
            blocks.put(block.name(), block);
            return this;
        }

        public BuildGradleTemplate build() {
            return new BuildGradleTemplate(List.copyOf(blockOrder), Map.copyOf(blocks));
        }
    }
}
