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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A simple closure block with no nested children (e.g., plugins, repositories, dependencies).
 * This is a leaf node in the block tree.
 *
 * This class combines the Block interface with the factory/descriptor pattern -
 * it knows how to create itself, parse itself, and match itself via regex.
 */
public record ClosureBlock(String name, String content) implements Block {

    @Override
    public Pattern pattern() {
        return Pattern.compile(
                "^\\s*" + Pattern.quote(name) + "\\s*\\{([^{}]*(?:\\{[^{}]*\\}[^{}]*)*)\\}",
                Pattern.MULTILINE | Pattern.DOTALL);
    }

    @Override
    public Block parse(String textContent) {
        return new ClosureBlock(name, normalizeIndentation(textContent.trim()));
    }

    /**
     * Normalize indentation by stripping ALL leading whitespace from each line.
     * Content should be stored without any indentation - indentation is added during rendering.
     */
    private static String normalizeIndentation(String textContent) {
        return textContent.isEmpty()
                ? textContent
                : textContent.lines().map(String::stripLeading).collect(Collectors.joining("\n"));
    }

    @Override
    public String render() {
        return content;
    }

    @Override
    public Block merge(Block other) {
        return other instanceof ClosureBlock o
                ? Stream.of(content, o.content)
                        .filter(c -> !c.isEmpty())
                        .collect(
                                Collectors.collectingAndThen(
                                        Collectors.joining("\n"), merged -> new ClosureBlock(name, merged)))
                : this;
    }

    @Override
    public Block edit(Function<String, String> editor) {
        return parse(editor.apply(render()));
    }
}
