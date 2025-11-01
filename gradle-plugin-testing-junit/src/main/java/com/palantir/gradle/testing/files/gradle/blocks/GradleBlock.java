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

import com.palantir.gradle.testing.files.gradle.GradleFile;
import com.palantir.gradle.testing.files.gradle.StructuredGradleFile;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.intellij.lang.annotations.Language;

/**
 * A GradleFile wrapper that operates on a specific block within the file.
 * <p>
 * Enables natural chaining for nested block access:
 * {@code buildGradle.buildscript().repositories().append("mavenCentral()")}
 * <p>
 * All operations (append, edit, etc.) are scoped to the wrapped block and automatically
 * update the parent file with proper formatting and block ordering.
 */
public class GradleBlock implements GradleFile {
    private final StructuredGradleFile root;
    private final String[] blockPath;

    protected final StructuredGradleFile getRoot() {
        return root;
    }

    protected final String[] getBlockPath() {
        return blockPath;
    }

    public GradleBlock(StructuredGradleFile root, String... path) {
        this.root = root;
        this.blockPath = path;
    }

    @Override
    public final Path path() {
        return root.path();
    }

    @Override
    public final String text() {
        ParsedContent parsed = root.parseContent(root.text());
        Block block = parsed.getBlockAt(blockTemplates(), blockPath);
        return block.renderContent();
    }

    @Override
    public final GradleBlock edit(FileEditor editor) {
        ParsedContent parsed = root.parseContent(root.text());
        Map<String, Block> templates = blockTemplates();

        Block block = parsed.getBlockAt(templates, blockPath);
        Block updated = block.edit(editor::edit);
        ParsedContent withUpdate = parsed.withBlockAt(templates, blockPath, updated);

        @Language("Gradle")
        String rendered = root.renderContent(withUpdate);
        root.overwrite(rendered);
        return this;
    }

    private Map<String, Block> blockTemplates() {
        return root.getBlocks().stream().collect(Collectors.toMap(Block::name, b -> b));
    }

    @Override
    public final GradleBlock append(String text) {
        return edit(existing -> existing.isEmpty() ? text : String.join("\n", existing, text));
    }

    @Override
    public final GradleBlock prepend(String text) {
        return edit(
                existing -> existing.isEmpty() ? text : text.endsWith("\n") ? text + existing : text + "\n" + existing);
    }

    @Override
    public final GradleBlock overwrite(String text) {
        return edit(_existing -> text);
    }

    protected static String[] concat(String[] base, String... additional) {
        return Stream.concat(Stream.of(base), Stream.of(additional)).toArray(String[]::new);
    }
}
