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

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Structured API for managing the {@code plugins {}} block in Gradle files.
 * <p>
 * Example: {@code rootProject.buildGradle().plugins().add("java").add("application")}
 *
 * @see GradleFile#plugins()
 */
public final class Plugins {
    // Matches: plugins { ... }
    // Captures everything between the braces
    private static final Pattern PLUGINS_BLOCK_PATTERN = Pattern.compile(
            "plugins" // literal "plugins"
                    + "\\s*" // optional whitespace
                    + "\\{" // opening brace
                    + "([^}]*)" // capture group: any chars except }, greedily
                    + "}" // closing brace
                    + "\\n?", // optional newline after
            Pattern.DOTALL);

    private static final Pattern BUILDSCRIPT_PATTERN = Pattern.compile("buildscript\\s*\\{");

    private final GradleFile file;

    Plugins(GradleFile file) {
        this.file = file;
    }

    /**
     * Adds a plugin without a version. Example: {@code add("java")}
     */
    public Plugins add(String pluginId) {
        return addPlugin(new Plugin(pluginId, Optional.empty()));
    }

    /**
     * Adds a plugin with apply false. Example: {@code addWithoutApply("com.example.plugin")}
     */
    public Plugins addWithoutApply(String pluginId) {
        return addPlugin(new Plugin(pluginId, Optional.of(false)));
    }

    private Plugins addPlugin(Plugin plugin) {
        file.edit(text -> addPlugin(text, plugin));
        return this;
    }

    private String addPlugin(String originalText, Plugin plugin) {
        List<Plugin> plugins = Stream.concat(parse(originalText).stream(), Stream.of(plugin))
                .distinct()
                .toList();

        return insertPluginsBlock(originalText, plugins);
    }

    private String insertPluginsBlock(String text, List<Plugin> plugins) {
        String textWithoutPluginsBlock = PLUGINS_BLOCK_PATTERN.matcher(text).replaceFirst("");
        String pluginsContent =
                plugins.stream().map(plugin -> "    " + plugin.toDeclaration()).collect(Collectors.joining("\n"));

        String pluginsBlock = "plugins {\n" + pluginsContent + "\n}\n";

        return repositionPluginsBlockWithContent(textWithoutPluginsBlock, pluginsBlock);
    }

    private static String repositionPluginsBlockWithContent(String textWithoutPluginsBlock, String pluginsBlock) {
        // Find all buildscript block ranges in a single pass
        List<int[]> ranges = BUILDSCRIPT_PATTERN
                .matcher(textWithoutPluginsBlock)
                .results()
                .map(match -> findMatchingBrace(textWithoutPluginsBlock, match.end() - 1)
                        .map(endIndex -> new int[] {match.start(), endIndex}))
                .takeWhile(Optional::isPresent)
                .flatMap(Optional::stream)
                .toList();

        // Extract buildscript blocks
        String buildscripts = ranges.stream()
                .map(range -> textWithoutPluginsBlock.substring(range[0], range[1]))
                .collect(Collectors.joining("\n"));

        // Build remaining text by subtracting ranges (reverse order to preserve indices)
        String remainingText = Lists.reverse(ranges).stream()
                .reduce(
                        textWithoutPluginsBlock,
                        (text, range) -> text.substring(0, range[0]) + text.substring(range[1]),
                        (_a, _b) -> _a);

        String cleanedRemaining = remainingText.replaceFirst("^\\n+", "");

        if (buildscripts.isEmpty()) {
            return pluginsBlock + cleanedRemaining;
        }

        return buildscripts + "\n" + pluginsBlock + cleanedRemaining;
    }

    /**
     * Parses plugins from Gradle file text, or returns empty list if no plugins block exists.
     */
    static List<Plugin> parse(String text) {
        Matcher matcher = PLUGINS_BLOCK_PATTERN.matcher(text);

        if (!matcher.find()) {
            return List.of();
        }

        return Stream.of(matcher.group(1).split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(Plugin::new)
                .toList();
    }

    /**
     * Repositions the plugins block to the correct location (at top or after buildscript).
     * If no plugins block exists, returns the text unchanged.
     */
    static String repositionPluginsBlock(String text) {
        List<Plugin> plugins = parse(text);
        if (plugins.isEmpty()) {
            return text;
        }

        String textWithoutPluginsBlock = PLUGINS_BLOCK_PATTERN.matcher(text).replaceFirst("");
        String pluginsContent =
                plugins.stream().map(plugin -> "    " + plugin.toDeclaration()).collect(Collectors.joining("\n"));

        String pluginsBlock = "plugins {\n" + pluginsContent + "\n}\n";

        return repositionPluginsBlockWithContent(textWithoutPluginsBlock, pluginsBlock);
    }

    private static Optional<Integer> findMatchingBrace(String text, int openBraceIndex) {
        int depth = 1;
        for (int i = openBraceIndex + 1; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
            }
            if (depth == 0) {
                return Optional.of(i + 1);
            }
        }
        return Optional.empty();
    }
}
