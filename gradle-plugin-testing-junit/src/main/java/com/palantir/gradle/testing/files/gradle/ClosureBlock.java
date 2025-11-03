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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A closure block containing child blocks and/or unstructured content.
 * <p>
 * Handles both simple blocks (empty children map) and nested blocks (non-empty children).
 * Child order is preserved via {@link LinkedHashMap} insertion order. Content is stored without
 * indentation and indented during rendering.
 * <p>
 * Examples:
 * <ul>
 *   <li>Simple: {@code plugins { id 'java' }} - empty children, content in {@code unstructuredContent}</li>
 *   <li>Nested: {@code buildscript { repositories { } }} - child blocks in {@code children} map</li>
 * </ul>
 *
 * @see Block
 * @see ParsedContent
 */
public record ClosureBlock(String name, Map<String, Block> children, String unstructuredContent) implements Block {

    @Override
    public Optional<ExtractionResult> extract(String content) {
        Pattern pattern = Pattern.compile("^\\s*" + Pattern.quote(name) + "\\s*\\{", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);

        return Optional.of(matcher).filter(Matcher::find).flatMap(m -> findMatchingBrace(content, m.end() - 1)
                .map(closePos -> new ExtractionResult(content.substring(m.end(), closePos), m.start(), closePos + 1)));
    }

    private static Optional<Integer> findMatchingBrace(String content, int openPos) {
        int depth = 1;
        for (int i = openPos + 1; i < content.length(); i++) {
            switch (content.charAt(i)) {
                case '{' -> depth++;
                case '}' -> {
                    if (--depth == 0) {
                        return Optional.of(i);
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Block parse(String content) {
        if (children.isEmpty()) {
            return new ClosureBlock(
                    name, children, removeLeadingAndTrailingBlankLines(content).stripIndent());
        }
        // Nested block - parse children
        List<String> childOrder = List.copyOf(children.keySet());
        ParsedContent.ParseState result = ParsedContent.parseBlocks(content, childOrder, children, true);
        return new ClosureBlock(name, result.parsedBlocks(), result.remaining().trim());
    }

    private static String removeLeadingAndTrailingBlankLines(String textContent) {
        return textContent.replaceFirst("^(?:\\s*\\n)+", "").replaceFirst("(?:\\n\\s*)+$", "");
    }

    @Override
    public String renderContent() {
        if (children.isEmpty()) {
            return unstructuredContent;
        }
        List<String> childOrder = List.copyOf(children.keySet());
        String renderedBlocks = renderBlocks(childOrder, children);
        return combineUnstructured(renderedBlocks, unstructuredContent);
    }

    @Override
    public String renderBlock() {
        return name + " {\n" + renderContent().indent(4).stripTrailing() + "\n}";
    }

    @Override
    public Block merge(Block other) {
        if (!(other instanceof ClosureBlock o)) {
            return this;
        }

        Map<String, Block> mergedChildren = children.keySet().stream()
                .map(childName -> {
                    Optional<Block> existing = Optional.ofNullable(children.get(childName));
                    Optional<Block> otherChild = Optional.ofNullable(o.children.get(childName));

                    return existing.flatMap(e -> otherChild.map(e::merge))
                            .or(() -> otherChild)
                            .or(() -> existing)
                            .map(block -> Map.entry(childName, block));
                })
                .<Map.Entry<String, Block>>mapMulti(Optional::ifPresent)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

        String mergedUnstructured = combineUnstructured(unstructuredContent, o.unstructuredContent);
        return new ClosureBlock(name, mergedChildren, mergedUnstructured);
    }

    @Override
    public Optional<Block> getChild(String childName) {
        return Optional.ofNullable(children.get(childName));
    }

    @Override
    public Block withChild(String childName, Block child) {
        Map<String, Block> updatedChildren = new LinkedHashMap<>(children);
        updatedChildren.put(childName, child);
        return new ClosureBlock(name, updatedChildren, unstructuredContent);
    }

    /**
     * Render blocks to text in the specified order.
     *
     * @param blockOrder the order to render blocks
     * @param blocks the blocks to render
     * @return rendered text with blocks joined by newlines
     */
    private static String renderBlocks(List<String> blockOrder, Map<String, Block> blocks) {
        return blockOrder.stream()
                .map(blocks::get)
                .filter(Objects::nonNull)
                .filter(block -> !block.renderContent().isEmpty())
                .map(block ->
                        block.name() + " {\n" + block.renderContent().indent(4).stripTrailing() + "\n}")
                .collect(Collectors.joining("\n"));
    }

    /**
     * Join non-empty strings with newline.
     */
    private static String combineUnstructured(String first, String second) {
        if (first.isEmpty()) {
            return second;
        }
        if (second.isEmpty()) {
            return first;
        }
        return first + "\n" + second;
    }
}
