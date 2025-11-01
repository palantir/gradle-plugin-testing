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

/**
 * Base class for all block implementations that represent a specific block within a GradleFile.
 * Blocks are themselves GradleFiles, allowing natural chaining like:
 * buildGradle.buildscript().repositories().append("mavenCentral()")
 */
public class GradleBlock implements GradleFile {
    protected final StructuredGradleFile root;
    protected final String[] blockPath;

    public GradleBlock(StructuredGradleFile root, String... path) {
        this.root = root;
        this.blockPath = path;
    }

    @Override
    public Path path() {
        return root.path();
    }

    @Override
    public String text() {
        ParsedContent parsed = root.parseContent(root.text());

        Map<String, Block> blockMap =
                root.getBlocks().stream().collect(Collectors.toMap(Block::name, b -> b));

        Block block = parsed.getBlockAt(blockMap, blockPath);
        return block.render();
    }

    @Override
    public GradleBlock edit(FileEditor editor) {
        ParsedContent parsed = root.parseContent(root.text());

        Map<String, Block> blockMap =
                root.getBlocks().stream().collect(Collectors.toMap(Block::name, b -> b));

        Block block = parsed.getBlockAt(blockMap, blockPath);
        Block updated = block.edit(editor::edit);
        ParsedContent withUpdate = parsed.withBlockAt(blockMap, blockPath, updated);

        String rendered = root.renderContent(withUpdate);
        root.overwrite(rendered);
        return this;
    }

    @Override
    public GradleBlock append(String text) {
        return edit(existing -> existing.isEmpty()
                ? text
                : Stream.of(existing, text).collect(Collectors.joining("\n")));
    }

    @Override
    public GradleBlock prepend(String text) {
        return edit(existing -> existing.isEmpty()
                ? text
                : text.endsWith("\n") ? text + existing : text + "\n" + existing);
    }

    @Override
    public GradleBlock overwrite(String text) {
        return edit(_existing -> text);
    }

    protected static String[] concat(String[] base, String... additional) {
        return Stream.concat(Stream.of(base), Stream.of(additional)).toArray(String[]::new);
    }
}
