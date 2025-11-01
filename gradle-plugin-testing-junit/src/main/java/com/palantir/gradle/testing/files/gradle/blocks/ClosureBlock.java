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

import java.util.regex.Pattern;

/**
 * A simple closure block with no nested children (e.g., plugins, repositories, dependencies).
 * This is a leaf node in the block tree.
 */
public record ClosureBlock(String name, String content) implements Block {

    @Override
    public Pattern pattern() {
        return Pattern.compile(
                "^\\s*" + Pattern.quote(name) + "\\s*\\{([^{}]*(?:\\{[^{}]*\\}[^{}]*)*)\\}",
                Pattern.MULTILINE | Pattern.DOTALL);
    }

    @Override
    public Block parse(String content) {
        // Strip leading/trailing whitespace and normalize indentation
        String normalized = normalizeIndentation(content.trim());
        return new ClosureBlock(name, normalized);
    }

    /**
     * Normalize indentation by stripping ALL leading whitespace from each line.
     * Content should be stored without any indentation - indentation is added during rendering.
     */
    private static String normalizeIndentation(String content) {
        if (content.isEmpty()) {
            return content;
        }

        return content.lines()
                .map(String::stripLeading)  // Remove all leading whitespace from each line
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    @Override
    public String render() {
        return content;
    }

    @Override
    public Block merge(Block other) {
        if (!(other instanceof ClosureBlock o)) {
            return this;
        }

        if (content.isEmpty()) {
            return o;
        }
        if (o.content.isEmpty()) {
            return this;
        }

        return new ClosureBlock(name, content + "\n" + o.content);
    }

    @Override
    public Block edit(java.util.function.Function<String, String> editor) {
        // Edit the rendered content, then parse it back
        String newContent = editor.apply(render());
        return parse(newContent);
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static final class Builder {
        private final String name;

        public Builder(String name) {
            this.name = name;
        }

        public ClosureBlock build() {
            return new ClosureBlock(name, "");
        }
    }
}
