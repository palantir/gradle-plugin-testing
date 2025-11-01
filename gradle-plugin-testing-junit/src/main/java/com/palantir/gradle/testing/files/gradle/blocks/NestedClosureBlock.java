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

import java.util.function.Function;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A closure block that contains nested child blocks (e.g., buildscript, configurations).
 * This is a composite node in the block tree that can contain both structured child blocks
 * and unstructured content.
 *
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
        class ParseState {
            String remaining;
            Map<String, Block> parsedChildren;

            ParseState(String remaining, Map<String, Block> parsedChildren) {
                this.remaining = remaining;
                this.parsedChildren = parsedChildren;
            }
        }

        ParseState result = childOrder.stream()
                .filter(children::containsKey)
                .reduce(
                        new ParseState(content, new HashMap<>(children)),
                        (state, childName) -> {
                            Block childTemplate = children.get(childName);
                            Matcher matcher = childTemplate.pattern().matcher(state.remaining);

                            if (matcher.find()) {
                                state.parsedChildren.put(childName, childTemplate.parse(matcher.group(1)));
                                state.remaining = state.remaining.substring(0, matcher.start())
                                        + state.remaining.substring(matcher.end());
                            }
                            return state;
                        },
                        (s1, s2) -> s1);

        return new NestedClosureBlock(name, childOrder, result.parsedChildren, result.remaining.trim());
    }

    @Override
    public String render() {
        return childOrder.stream()
                .map(children::get)
                .filter(child -> child != null && !child.render().isEmpty())
                .map(child -> child.name() + " {\n" + indent(child.render()) + "\n}")
                .collect(Collectors.collectingAndThen(
                        Collectors.joining("\n"),
                        result -> unstructuredContent.isEmpty()
                                ? result
                                : (result.isEmpty() ? unstructuredContent : result + "\n" + unstructuredContent)));
    }

    @Override
    public Block merge(Block other) {
        if (!(other instanceof NestedClosureBlock o)) {
            return this;
        }

        Map<String, Block> mergedChildren = childOrder.stream()
                .collect(Collectors.toMap(
                        childName -> childName,
                        childName -> {
                            Block existingChild = children.get(childName);
                            Block otherChild = o.children.get(childName);
                            if (existingChild != null && otherChild != null) {
                                return existingChild.merge(otherChild);
                            }
                            return otherChild != null ? otherChild : existingChild;
                        }));

        String mergedUnstructured = combineUnstructured(unstructuredContent, o.unstructuredContent);
        return new NestedClosureBlock(name, childOrder, mergedChildren, mergedUnstructured);
    }

    private String combineUnstructured(String a, String b) {
        return Stream.of(a, b)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("\n"));
    }

    @Override
    public Block edit(Function<String, String> editor) {
        return parse(editor.apply(render()));
    }

    private String indent(String content) {
        return content.lines().map(line -> line.isEmpty() ? line : "    " + line).collect(Collectors.joining("\n"));
    }
}
