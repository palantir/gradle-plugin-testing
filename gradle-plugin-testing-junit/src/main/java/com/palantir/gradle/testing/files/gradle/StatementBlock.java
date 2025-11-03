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

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A block representing a collection of similar statements (e.g., include or apply plugin).
 * <p>
 * Contains multiple statements of the same type, one per line.
 * When merging, combines all unique statements from both blocks.
 *
 * @see Block
 */
non-sealed class StatementBlock implements Block {
    private final String name;
    private final String keyword;
    private final Pattern linePattern;
    private final Set<String> statements;

    StatementBlock(String name, String keyword, Set<String> statements) {
        this.name = name;
        this.keyword = keyword;
        this.statements = statements;
        this.linePattern = Pattern.compile(Pattern.quote(keyword) + "\\s+'([^']+)'");
    }

    @Override
    public final String name() {
        return name;
    }

    public final Pattern pattern() {
        // Match all statements of this type at this level (greedy to capture multiple lines)
        return Pattern.compile("((?:^" + Pattern.quote(keyword) + "\\s+'[^']+'\\s*\n*)+)", Pattern.MULTILINE);
    }

    @Override
    public final Optional<ExtractionResult> extract(String content) {
        Matcher matcher = pattern().matcher(content);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new ExtractionResult(matcher.group(1), matcher.start(), matcher.end()));
    }

    @Override
    public final Block parse(String content) {
        Set<String> parsed = linePattern
                .matcher(content)
                .results()
                .map(match -> match.group(1))
                .collect(Collectors.toSet());
        return new StatementBlock(name, keyword, parsed);
    }

    @Override
    public final String renderContent() {
        return statements.stream()
                .sorted()
                .map(statement -> keyword + " '" + statement + "'")
                .collect(Collectors.joining("\n"));
    }

    @Override
    public final String renderBlock() {
        return renderContent();
    }

    @Override
    public final Block merge(Block other) {
        if (!(other instanceof StatementBlock otherBlock) || !otherBlock.name.equals(this.name)) {
            return this;
        }
        // Combine all unique statements from both blocks
        Set<String> merged = Stream.concat(this.statements.stream(), otherBlock.statements.stream())
                .collect(Collectors.toSet());
        return new StatementBlock(name, keyword, merged);
    }

    @Override
    public final Optional<Block> getChild(String childName) {
        return Optional.empty();
    }

    @Override
    public final Block withChild(String childName, Block child) {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support children");
    }
}
