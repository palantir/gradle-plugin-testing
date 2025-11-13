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

    // Matches: buildscript { ... } with ONE level of nested braces allowed
    private static final Pattern BUILDSCRIPT_BLOCK_PATTERN = Pattern.compile(
            "buildscript" // literal "buildscript"
                    + "\\s*" // optional whitespace
                    + "\\{" // opening brace
                    + "(?:" // non-capturing group, repeated:
                    + "[^{}]" //   either: any char that's not a brace
                    + "|" //   OR
                    + "\\{[^{}]*}" //   a pair of braces with non-brace content inside
                    + ")*" // repeat the group zero or more times
                    + "}", // closing brace
            Pattern.DOTALL);

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
        Matcher buildscriptMatcher = BUILDSCRIPT_BLOCK_PATTERN.matcher(textWithoutPluginsBlock);

        // plugins block must go after buildscript and above all else
        if (buildscriptMatcher.find()) {
            int insertPosition = buildscriptMatcher.end();
            String suffix = textWithoutPluginsBlock.substring(insertPosition).replaceFirst("^\n", "");
            return textWithoutPluginsBlock.substring(0, insertPosition) + "\n" + pluginsBlock + suffix;
        }

        return pluginsBlock + textWithoutPluginsBlock;
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
}
