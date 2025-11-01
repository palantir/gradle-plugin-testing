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

import com.palantir.gradle.testing.files.gradle.blocks.Block;
import com.palantir.gradle.testing.files.gradle.blocks.BlockEditor;
import com.palantir.gradle.testing.files.gradle.blocks.ClosureBlock;
import com.palantir.gradle.testing.files.gradle.blocks.ParsedContent;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base class for structured Gradle files ({@code build.gradle}, {@code settings.gradle}).
 * <p>
 * Handles parsing, rendering, and block navigation using block definitions.
 * Subclasses define their block structure via {@link #blocks()}, and this class
 * delegates to {@link ParsedContent} for parsing and rendering operations.
 * <p>
 * The file structure is preserved during edits - blocks maintain their order
 * and unstructured content is preserved alongside structured blocks.
 *
 * @see BuildGradleFile
 * @see SettingsGradleFile
 * @see ParsedContent
 * @see Block
 */
public abstract class StructuredGradleFile implements GradleFile {
    private final Path path;

    protected StructuredGradleFile(Path path) {
        this.path = path;
    }

    /**
     * Subclasses define their block structure.
     * Block order is preserved during parsing and rendering.
     *
     * @return list of {@link Block} definitions for this file type
     */
    protected abstract List<Block> blocks();

    /**
     * Block order derived from {@link #blocks()}.
     *
     * @return list of block names in order
     */
    protected List<String> blockOrder() {
        return blocks().stream().map(Block::name).collect(Collectors.toList());
    }

    /**
     * Helper to create simple closure block without children.
     *
     * @param name the block name (e.g., {@code "plugins"}, {@code "repositories"})
     * @return a {@link ClosureBlock} with no children
     */
    protected static Block closure(String name) {
        return new ClosureBlock(name, Map.of(), "");
    }

    /**
     * Helper to create nested closure block with child blocks.
     *
     * @param name the block name (e.g., {@code "buildscript"})
     * @param children the child blocks this block can contain
     * @return a {@link ClosureBlock} with the specified children
     */
    protected static Block nested(String name, Block... children) {
        return new ClosureBlock(
                name,
                Stream.of(children)
                        .collect(Collectors.toMap(Block::name, b -> b, (a, b) -> a, java.util.LinkedHashMap::new)),
                "");
    }

    /**
     * Public accessor for blocks - needed by {@link BlockEditor}.
     *
     * @return list of {@link Block} definitions for this file type
     */
    public List<Block> getBlocks() {
        return blocks();
    }

    @Override
    public final Path path() {
        return path;
    }

    @Override
    public final String text() {
        try {
            return Files.exists(path) ? Files.readString(path) : "";
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ParsedContent parse(String content) {
        Map<String, Block> blockMap = blocks().stream().collect(Collectors.toMap(Block::name, b -> b));
        return ParsedContent.parseContent(content, blockOrder(), blockMap);
    }

    private String render(ParsedContent content) {
        return ParsedContent.renderContent(content, blockOrder());
    }

    @Override
    public final StructuredGradleFile append(String text) {
        ParsedContent existing = parse(text());
        ParsedContent toAppend = parse(text);
        ParsedContent merged = existing.merge(toAppend);
        overwrite(render(merged));
        return this;
    }

    @Override
    public final StructuredGradleFile prepend(String text) {
        ParsedContent existing = parse(text());
        ParsedContent toPrepend = parse(text);
        ParsedContent merged = toPrepend.merge(existing);
        overwrite(render(merged));
        return this;
    }

    @Override
    public final StructuredGradleFile edit(FileEditor editor) {
        ParsedContent parsed = parse(text());
        String edited = editor.edit(render(parsed));
        ParsedContent reparsed = parse(edited);
        overwrite(render(reparsed));
        return this;
    }
}
