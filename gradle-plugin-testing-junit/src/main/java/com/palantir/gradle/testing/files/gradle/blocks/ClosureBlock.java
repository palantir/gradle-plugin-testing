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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
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
    public Pattern pattern() {
        return Pattern.compile(
                "^\\s*" + Pattern.quote(name) + "\\s*\\{([^{}]*(?:\\{[^{}]*\\}[^{}]*)*)\\}",
                Pattern.MULTILINE | Pattern.DOTALL);
    }

    @Override
    public Block parse(String content) {
        if (children.isEmpty()) {
            // Simple block - just store normalized content
            return new ClosureBlock(name, children, normalizeIndentation(content.trim()));
        }
        // Nested block - parse children
        List<String> childOrder = List.copyOf(children.keySet());
        ParsedContent.ParseState result = ParsedContent.parseBlocks(content, childOrder, children, true);
        return new ClosureBlock(name, result.parsedBlocks(), result.remaining().trim());
    }

    /**
     * Strip leading whitespace from each line so content is stored without indentation.
     */
    private static String normalizeIndentation(String textContent) {
        return textContent.isEmpty()
                ? textContent
                : textContent.lines().map(String::stripLeading).collect(Collectors.joining("\n"));
    }

    @Override
    public String renderContent() {
        if (children.isEmpty()) {
            return unstructuredContent;
        }
        List<String> childOrder = List.copyOf(children.keySet());
        String renderedBlocks = ParsedContent.renderBlocks(childOrder, children, true);
        return ParsedContent.combineUnstructured(renderedBlocks, unstructuredContent);
    }

    @Override
    public String renderBlock() {
        return name + " {\n" + ParsedContent.indent(renderContent()) + "\n}";
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

        String mergedUnstructured = ParsedContent.combineUnstructured(unstructuredContent, o.unstructuredContent);
        return new ClosureBlock(name, mergedChildren, mergedUnstructured);
    }

    @Override
    public Block edit(Function<String, String> editor) {
        return parse(editor.apply(renderContent()));
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
}
