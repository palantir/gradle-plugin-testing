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
 * A closure block that contains nested child blocks (e.g., buildscript, configurations).
 * This is a composite node in the block tree that can contain both structured child blocks
 * and unstructured content.
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
        String remaining = content;
        // Start with all template children (preserves structure even if not in content)
        Map<String, Block> parsedChildren = new HashMap<>(children);

        // Each child parses itself using its own pattern
        for (String childName : childOrder) {
            Block childTemplate = children.get(childName);
            if (childTemplate == null) {
                continue;
            }

            Pattern pattern = childTemplate.pattern();
            Matcher matcher = pattern.matcher(remaining);

            if (matcher.find()) {
                String innerContent = matcher.group(1);
                // Update the parsed version
                parsedChildren.put(childName, childTemplate.parse(innerContent));

                // Remove matched content
                remaining = remaining.substring(0, matcher.start()) + remaining.substring(matcher.end());
            }
            // If not found, childTemplate (empty) is already in parsedChildren from the initialization
        }

        return new NestedClosureBlock(name, childOrder, parsedChildren, remaining.trim());
    }

    @Override
    public String render() {
        List<String> parts = new ArrayList<>();

        // Render each child in order
        for (String childName : childOrder) {
            Block child = children.get(childName);
            if (child == null) {
                continue;
            }

            String childContent = child.render();
            if (!childContent.isEmpty()) {
                // child.render() returns the raw content without wrapping
                // We need to indent it and wrap it in braces
                parts.add(childName + " {\n" + indent(childContent) + "\n}");
            }
        }

        // Add unstructured content (already properly formatted)
        if (!unstructuredContent.isEmpty()) {
            parts.add(unstructuredContent);
        }

        // Join without extra blank lines - parent (template) will add spacing between top-level blocks
        return String.join("\n", parts);
    }

    @Override
    public Block merge(Block other) {
        if (!(other instanceof NestedClosureBlock o)) {
            return this;
        }

        Map<String, Block> mergedChildren = new HashMap<>(children);

        // Each child merges itself recursively
        for (String childName : childOrder) {
            Block existingChild = mergedChildren.get(childName);
            Block otherChild = o.children.get(childName);

            if (existingChild != null && otherChild != null) {
                mergedChildren.put(childName, existingChild.merge(otherChild));
            } else if (otherChild != null) {
                mergedChildren.put(childName, otherChild);
            }
        }

        // Combine unstructured content
        String mergedUnstructured = combineUnstructured(unstructuredContent, o.unstructuredContent);

        return new NestedClosureBlock(name, childOrder, mergedChildren, mergedUnstructured);
    }

    private String combineUnstructured(String a, String b) {
        if (a.isEmpty()) {
            return b;
        }
        if (b.isEmpty()) {
            return a;
        }
        return a + "\n" + b;
    }

    @Override
    public Block edit(java.util.function.Function<String, String> editor) {
        // Edit the rendered content, then parse it back
        String newContent = editor.apply(render());
        return parse(newContent);
    }

    private String indent(String content) {
        return content.lines().map(line -> line.isEmpty() ? line : "    " + line).collect(Collectors.joining("\n"));
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static final class Builder {
        private final String name;
        private final List<String> childOrder = new ArrayList<>();
        private final Map<String, Block> children = new HashMap<>();

        public Builder(String name) {
            this.name = name;
        }

        public Builder child(String childName) {
            childOrder.add(childName);
            children.put(childName, ClosureBlock.builder(childName).build());
            return this;
        }

        public Builder child(Block block) {
            childOrder.add(block.name());
            children.put(block.name(), block);
            return this;
        }

        public NestedClosureBlock build() {
            return new NestedClosureBlock(name, List.copyOf(childOrder), Map.copyOf(children), "");
        }
    }
}
