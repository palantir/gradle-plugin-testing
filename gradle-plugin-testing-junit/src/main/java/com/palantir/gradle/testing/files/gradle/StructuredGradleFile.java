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
import com.palantir.gradle.testing.files.gradle.blocks.ParsedContent;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Base class for structured Gradle files (build.gradle, settings.gradle).
 * Handles parsing, rendering, and block navigation using block definitions.
 */
public abstract class StructuredGradleFile implements GradleFile {
    private final Path path;

    protected StructuredGradleFile(Path path) {
        this.path = path;
    }

    /** Subclasses define their block structure */
    protected abstract List<Block> blocks();

    /** Subclasses define block order for rendering */
    protected abstract List<String> blockOrder();

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
    public Path path() {
        return path;
    }

    @Override
    public String text() {
        try {
            return Files.exists(path) ? Files.readString(path) : "";
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected ParsedContent parse(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new ParsedContent(Map.of(), "");
        }

        Map<String, Block> blockMap = blocks().stream().collect(Collectors.toMap(Block::name, b -> b));

        class ParseState {
            String remaining;
            Map<String, Block> parsedBlocks;

            ParseState(String remaining, Map<String, Block> parsedBlocks) {
                this.remaining = remaining;
                this.parsedBlocks = parsedBlocks;
            }
        }

        ParseState result = blockOrder().stream()
                .filter(blockMap::containsKey)
                .reduce(
                        new ParseState(content, new HashMap<>()),
                        (state, blockName) -> {
                            Block template = blockMap.get(blockName);
                            Matcher matcher = template.pattern().matcher(state.remaining);

                            if (matcher.find()) {
                                state.parsedBlocks.put(blockName, template.parse(matcher.group(1)));
                                state.remaining = state.remaining.substring(0, matcher.start())
                                        + state.remaining.substring(matcher.end());
                            }
                            return state;
                        },
                        (s1, s2) -> s1);

        return new ParsedContent(result.parsedBlocks, normalizeRemaining(result.remaining));
    }

    protected String render(ParsedContent content) {
        String blocks = blockOrder().stream()
                .map(content.blocks()::get)
                .filter(block -> block != null && !block.render().isEmpty())
                .map(block -> block.name() + " {\n" + indent(block.render()) + "\n}")
                .collect(Collectors.joining("\n\n"));

        String result = content.unstructuredContent().isEmpty()
                ? blocks
                : (blocks.isEmpty() ? content.unstructuredContent() : blocks + "\n\n" + content.unstructuredContent());

        return result.isEmpty() || result.endsWith("\n") ? result : result + "\n";
    }

    @Override
    public StructuredGradleFile append(String text) {
        ParsedContent existing = parse(text());
        ParsedContent toAppend = parse(text);
        ParsedContent merged = existing.merge(toAppend);
        overwrite(render(merged));
        return this;
    }

    @Override
    public StructuredGradleFile edit(FileEditor editor) {
        ParsedContent parsed = parse(text());
        String edited = editor.edit(render(parsed));
        ParsedContent reparsed = parse(edited);
        overwrite(render(reparsed));
        return this;
    }

    private String indent(String content) {
        return content == null || content.isEmpty()
                ? ""
                : content.lines().map(line -> line.isEmpty() ? line : "    " + line).collect(Collectors.joining("\n"));
    }

    private String normalizeRemaining(String remaining) {
        return remaining.trim().replaceAll("\n{2,}", "\n");
    }
}
