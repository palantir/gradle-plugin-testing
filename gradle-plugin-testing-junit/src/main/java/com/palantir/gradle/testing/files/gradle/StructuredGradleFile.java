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
import com.palantir.gradle.testing.files.gradle.blocks.ClosureBlock;
import com.palantir.gradle.testing.files.gradle.blocks.ParsedContent;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base class for structured Gradle files (build.gradle, settings.gradle).
 * Handles parsing, rendering, and block navigation using block definitions.
 */
public abstract class StructuredGradleFile implements GradleFile {
    private final Path path;

    protected StructuredGradleFile(Path path) {
        this.path = path;
    }

    /** Subclasses define their block structure - order is preserved */
    protected abstract List<Block> blocks();

    /** Block order derived from blocks() */
    protected List<String> blockOrder() {
        return blocks().stream().map(Block::name).collect(Collectors.toList());
    }

    /** Helper to create simple closure block */
    protected static Block closure(String name) {
        return new ClosureBlock(name, Map.of(), "");
    }

    /** Helper to create nested closure block */
    protected static Block nested(String name, Block... children) {
        return new ClosureBlock(
                name,
                Stream.of(children)
                        .collect(Collectors.toMap(Block::name, b -> b, (a, b) -> a, java.util.LinkedHashMap::new)),
                "");
    }

    /** Public accessor for blocks - needed by GradleBlock */
    public List<Block> getBlocks() {
        return blocks();
    }

    /** Public accessor for parse - needed by GradleBlock */
    public ParsedContent parseContent(String content) {
        return parse(content);
    }

    /** Public accessor for render - needed by GradleBlock */
    public String renderContent(ParsedContent content) {
        return render(content);
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

    protected final ParsedContent parse(String content) {
        return Optional.ofNullable(content)
                .filter(c -> !c.trim().isEmpty())
                .map(this::doParse)
                .orElseGet(() -> new ParsedContent(Map.of(), ""));
    }

    private ParsedContent doParse(String content) {
        Map<String, Block> blockMap = blocks().stream().collect(Collectors.toMap(Block::name, b -> b));
        ParsedContent.ParseState result = ParsedContent.parseBlocks(content, blockOrder(), blockMap, false);
        return new ParsedContent(result.parsedBlocks(), normalizeRemaining(result.remaining()));
    }

    protected final String render(ParsedContent content) {
        String blocks = blockOrder().stream()
                .map(content.blocks()::get)
                .flatMap(block -> Optional.ofNullable(block).stream())
                .filter(block -> !block.renderContent().isEmpty())
                .map(Block::renderBlock)
                .collect(Collectors.joining("\n\n"));

        String result = content.unstructuredContent().isEmpty()
                ? blocks
                : (blocks.isEmpty() ? content.unstructuredContent() : blocks + "\n\n" + content.unstructuredContent());

        return result.isEmpty() || result.endsWith("\n") ? result : result + "\n";
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
    public final StructuredGradleFile edit(FileEditor editor) {
        ParsedContent parsed = parse(text());
        String edited = editor.edit(render(parsed));
        ParsedContent reparsed = parse(edited);
        overwrite(render(reparsed));
        return this;
    }

    private String normalizeRemaining(String remaining) {
        return remaining.trim().replaceAll("\n{2,}", "\n");
    }
}
