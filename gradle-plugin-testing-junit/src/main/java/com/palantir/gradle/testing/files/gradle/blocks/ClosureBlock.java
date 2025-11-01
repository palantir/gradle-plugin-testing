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
 * A closure block that may contain child blocks and unstructured content.
 * <p>
 * This unified block type handles both simple blocks (plugins, repositories) and nested blocks
 * (buildscript, configurations). Simple blocks have empty children; nested blocks define child order
 * via LinkedHashMap insertion order.
 * <p>
 * Examples:
 * - Simple: plugins { id 'java' }
 * - Nested: buildscript { repositories { } dependencies { } }
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
                .collect(Collectors.toMap(
                        childName -> childName,
                        childName -> {
                            Block existing = children.get(childName);
                            Block otherChild = o.children.get(childName);

                            if (existing != null && otherChild != null) {
                                return existing.merge(otherChild);
                            }
                            return otherChild != null ? otherChild : existing;
                        },
                        (a, b) -> a,
                        LinkedHashMap::new));

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
