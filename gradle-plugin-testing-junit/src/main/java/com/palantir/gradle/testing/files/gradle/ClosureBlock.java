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
public record ClosureBlock(
        String name, Map<String, List<Block>> children, String unstructuredContent, boolean shouldMerge)
        implements Block {

    @Override
    public Optional<ExtractionResult> extract(String content) {
        Pattern pattern = Pattern.compile("^\\s*" + Pattern.quote(name) + "\\s*\\{", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);

        return Optional.of(matcher).filter(Matcher::find).flatMap(m -> findMatchingBrace(content, m.end() - 1)
                .map(closePos -> new ExtractionResult(content.substring(m.end(), closePos), m.start(), closePos + 1)));
    }

    /**
     * Find the position of the closing brace matching the opening brace at openPos.
     * Simple depth-tracking algorithm - much clearer than stream reduction.
     */
    private static Optional<Integer> findMatchingBrace(String content, int openPos) {
        int depth = 1;
        for (int i = openPos + 1; i < content.length(); i++) {
            char chr = content.charAt(i);
            if (chr == '{') {
                depth++;
            } else if (chr == '}') {
                depth--;
                if (depth == 0) {
                    return Optional.of(i);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Block parse(String content) {
        return Optional.of(children)
                .filter(c -> !c.isEmpty())
                .map(c -> parseNestedBlock(content))
                .orElseGet(() -> parseSimpleBlock(content));
    }

    /**
     * Parse a simple closure block (no nested children).
     */
    private Block parseSimpleBlock(String content) {
        return new ClosureBlock(
                name, children, removeLeadingAndTrailingBlankLines(content).stripIndent(), shouldMerge);
    }

    /**
     * Parse a nested closure block with child blocks.
     */
    private Block parseNestedBlock(String content) {
        List<String> childOrder = List.copyOf(children.keySet());
        Map<String, Block> templates = children.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));

        ParseResult result = BlockOperations.parseNestedBlocks(content, childOrder, templates);
        String parsedUnstructured =
                removeLeadingAndTrailingBlankLines(result.remaining()).stripIndent();
        return new ClosureBlock(name, result.blocks(), parsedUnstructured, shouldMerge);
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
        String renderedBlocks = childOrder.stream()
                .filter(children::containsKey)
                .flatMap(blockName -> children.get(blockName).stream())
                .filter(block -> !block.renderContent().isEmpty())
                .map(block ->
                        block.name() + " {\n" + block.renderContent().indent(4).stripTrailing() + "\n}")
                .collect(Collectors.joining("\n"));
        return combineUnstructured(renderedBlocks, unstructuredContent);
    }

    @Override
    public String renderBlock() {
        return name + " {\n" + renderContent().indent(4).stripTrailing() + "\n}";
    }

    @Override
    public List<Block> merge(Block other) {
        if (!(other instanceof ClosureBlock o)) {
            return List.of(this, other);
        }

        if (!shouldMerge) {
            return List.of(this, other);
        }

        Map<String, List<Block>> mergedChildren = BlockOperations.mergeBlockLists(children, o.children);
        String mergedUnstructured = combineUnstructured(unstructuredContent, o.unstructuredContent);
        return List.of(new ClosureBlock(name, mergedChildren, mergedUnstructured, true));
    }

    @Override
    public Optional<Block> getChild(String childName) {
        return BlockOperations.firstBlock(children.get(childName));
    }

    @Override
    public Block withChild(String childName, Block child) {
        Map<String, List<Block>> updatedChildren = new LinkedHashMap<>(children);
        updatedChildren.put(childName, BlockOperations.updateBlockAt(children.get(childName), 0, child));
        return new ClosureBlock(name, updatedChildren, unstructuredContent, shouldMerge);
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
