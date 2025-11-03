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

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.intellij.lang.annotations.Language;

/**
 * A view onto a specific block within a {@link GradleFile}, enabling block-scoped operations.
 * <p>
 * Supports natural chaining for nested access:
 * <pre>{@code
 * buildGradle.buildscript().repositories().append("mavenCentral()")
 * }</pre>
 * <p>
 * All operations automatically update the parent file with proper formatting and block ordering.
 * This class delegates to {@link ParsedContent} for parsing and rendering operations.
 *
 * @see GradleFile
 * @see ParsedContent
 * @see Block
 */
public class BlockEditor implements GradleFile {
    private final StructuredGradleFile root;
    private final String[] blockPath;

    final StructuredGradleFile root() {
        return root;
    }

    final String[] blockPath() {
        return blockPath;
    }

    public BlockEditor(StructuredGradleFile root, String... path) {
        this.root = root;
        this.blockPath = path;
    }

    @Override
    public final Path path() {
        return root.path();
    }

    @Override
    public final String text() {
        Map<String, Block> templates = blockTemplates();
        ParsedContent parsed = ParsedContent.parseContent(root.text(), root.blockOrder(), templates);
        Block block = parsed.getBlockAt(templates, blockPath);
        return block.renderContent();
    }

    @Override
    public final BlockEditor edit(FileEditor editor) {
        Map<String, Block> templates = blockTemplates();
        ParsedContent parsed = ParsedContent.parseContent(root.text(), root.blockOrder(), templates);

        Block block = parsed.getBlockAt(templates, blockPath);
        Block updatedBlock = block.edit(editor::edit);
        ParsedContent updatedTree = parsed.withBlockAt(templates, blockPath, updatedBlock);

        @Language("Gradle")
        String rendered = ParsedContent.renderContent(updatedTree, root.blockOrder());
        root.overwrite(rendered);
        return this;
    }

    private Map<String, Block> blockTemplates() {
        return root.blocks().stream().collect(Collectors.toMap(Block::name, b -> b));
    }

    @Override
    public final BlockEditor append(String text) {
        return edit(existing -> existing.isEmpty() ? text : String.join("\n", existing, text));
    }

    @Override
    public final BlockEditor prepend(String text) {
        return edit(
                existing -> existing.isEmpty() ? text : text.endsWith("\n") ? text + existing : text + "\n" + existing);
    }

    @Override
    public final BlockEditor overwrite(String text) {
        return edit(_existing -> text);
    }

    static String[] concat(String[] base, String... additional) {
        return Stream.concat(Stream.of(base), Stream.of(additional)).toArray(String[]::new);
    }
}
