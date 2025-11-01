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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A closure block that contains nested child blocks (e.g., buildscript, configurations).
 * This is a composite node in the block tree that can contain both structured child blocks
 * and unstructured content.
 * <p>
 * This class combines the Block interface with the factory/descriptor pattern -
 * it knows how to create itself, parse itself, and match itself via regex.
 */
public record NestedClosureBlock(
        String name, List<String> childOrder, Map<String, Block> children, String unstructuredContent)
        implements Block {

    @Override
    public Pattern pattern() {
        return Pattern.compile(
                "^\\s*" + Pattern.quote(name) + "\\s*\\{([^{}]*(?:\\{[^{}]*\\}[^{}]*)*)\\}",
                Pattern.MULTILINE | Pattern.DOTALL);
    }

    @Override
    public Block parse(String content) {
        ParsedContent.ParseState result = ParsedContent.parseBlocks(content, childOrder, children, true);
        return new NestedClosureBlock(name, childOrder, result.parsedBlocks(), result.remaining().trim());
    }

    @Override
    public String render() {
        String renderedBlocks = ParsedContent.renderBlocks(childOrder, children, true);
        return ParsedContent.combineUnstructured(renderedBlocks, unstructuredContent);
    }

    @Override
    public Block merge(Block other) {
        if (!(other instanceof NestedClosureBlock o)) {
            return this;
        }

        Map<String, Block> mergedChildren = childOrder.stream()
                .collect(Collectors.toMap(childName -> childName, childName -> {
                    Optional<Block> existingChild = Optional.ofNullable(children.get(childName));
                    Optional<Block> otherChild = Optional.ofNullable(o.children.get(childName));

                    return existingChild
                            .flatMap(existing -> otherChild.map(existing::merge))
                            .or(() -> otherChild)
                            .or(() -> existingChild)
                            .orElseGet(() -> children.get(childName));
                }));

        String mergedUnstructured = ParsedContent.combineUnstructured(unstructuredContent, o.unstructuredContent);
        return new NestedClosureBlock(name, childOrder, mergedChildren, mergedUnstructured);
    }

    @Override
    public Block edit(Function<String, String> editor) {
        return parse(editor.apply(render()));
    }
}
