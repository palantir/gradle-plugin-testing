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

import java.util.List;
import java.util.Optional;
import java.util.Set;
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
final class StatementBlock implements Block {
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

    /**
     * Create a new StatementBlock with the given statements, reusing this block's name and keyword.
     *
     * @param newStatements the statements to include
     * @return a new StatementBlock with the given statements
     */
    StatementBlock withStatements(String... newStatements) {
        return new StatementBlock(name, keyword, Set.of(newStatements));
    }

    @Override
    public String name() {
        return name;
    }

    public Pattern pattern() {
        // Match all statements of this type at this level (greedy to capture multiple lines)
        return Pattern.compile("((?:^" + Pattern.quote(keyword) + "\\s+'[^']+'\\s*\n*)+)", Pattern.MULTILINE);
    }

    @Override
    public Optional<ExtractionResult> extract(String content) {
        Matcher matcher = pattern().matcher(content);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new ExtractionResult(matcher.group(1), matcher.start(), matcher.end()));
    }

    @Override
    public Block parse(String content) {
        Set<String> parsed = linePattern
                .matcher(content)
                .results()
                .map(match -> match.group(1))
                .collect(Collectors.toSet());
        return new StatementBlock(name, keyword, parsed);
    }

    @Override
    public String renderContent() {
        return statements.stream()
                .sorted()
                .map(statement -> keyword + " '" + statement + "'")
                .collect(Collectors.joining("\n"));
    }

    @Override
    public String renderBlock() {
        return renderContent();
    }

    @Override
    public List<Block> merge(Block other) {
        return Optional.of(other)
                .filter(StatementBlock.class::isInstance)
                .map(StatementBlock.class::cast)
                .filter(o -> o.name.equals(this.name))
                .map(o -> Stream.concat(this.statements.stream(), o.statements.stream())
                        .collect(Collectors.toSet()))
                .map(merged -> new StatementBlock(name, keyword, merged))
                .<List<Block>>map(List::of)
                .orElseGet(() -> List.of(this, other));
    }
}
