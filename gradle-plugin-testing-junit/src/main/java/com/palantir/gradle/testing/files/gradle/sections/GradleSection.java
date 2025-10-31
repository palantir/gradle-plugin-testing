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

package com.palantir.gradle.testing.files.gradle.sections;

import com.palantir.gradle.testing.files.ProjectFile;
import com.palantir.gradle.testing.files.gradle.GradleFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Base class for structured sections within Gradle files.
 * Implements GradleFile so all file operations work within the section's block context.
 * Uses ALL_SECTIONS for canonical ordering across all Gradle file types.
 *
 * Content is stored WITHOUT indentation during manipulation, and formatted only when
 * writing to the file system.
 */
public class GradleSection<T extends GradleFile> implements GradleFile {
    // Master list of all sections in canonical order - used for top-level relative ordering
    protected static final List<String> ALL_SECTIONS = List.of(
            "pluginManagement", "buildscript", "plugins", "allprojects", "subprojects", "repositories", "dependencies");

    // Nested sections have their own ordering (repositories always comes first in nested contexts)
    private static final List<String> NESTED_SECTIONS = List.of("repositories", "dependencies", "plugins");

    private final T gradleFile;
    private final String blockName;
    private final Optional<GradleSection<T>> parentSection;

    public GradleSection(T gradleFile, String blockName) {
        this(gradleFile, Optional.empty(), blockName);
    }

    public GradleSection(T gradleFile, GradleSection<T> parentSection, String blockName) {
        this(gradleFile, Optional.of(parentSection), blockName);
    }

    private GradleSection(T gradleFile, Optional<GradleSection<T>> parentSection, String blockName) {
        this.gradleFile = gradleFile;
        this.parentSection = parentSection;
        this.blockName = blockName;
    }

    // GradleFile interface - operates within block context
    @Override
    public final Path path() {
        return gradleFile.path();
    }

    @Override
    public final String text() {
        if (!Files.exists(path()) || gradleFile.text().isEmpty()) {
            return "";
        }
        return extractBlockContent(gradleFile.text());
    }

    @Override
    public final GradleSection<T> edit(ProjectFile.FileEditor editor) {
        parentSection.ifPresentOrElse(
                parent -> parent.edit(
                        parentContent -> updateBlock(parentContent, editor.edit(extractBlockContent(parentContent)))),
                () -> {
                    String fileContent = Files.exists(path()) ? gradleFile.text() : "";
                    String unformatted = updateBlock(fileContent, editor.edit(extractBlockContent(fileContent)));
                    gradleFile.overwrite(formatGradleContent(unformatted));
                });
        return this;
    }

    @Override
    public final GradleSection<T> append(String content) {
        return edit(existing -> {
            if (existing.isEmpty()) {
                return content;
            }
            return existing + "\n" + content;
        });
    }

    @Override
    public final GradleSection<T> prepend(String content) {
        return edit(existing -> {
            if (existing.isEmpty()) {
                return content;
            }
            return content + "\n" + existing;
        });
    }

    @Override
    public final GradleSection<T> overwrite(String content) {
        return edit(_existing -> content);
    }

    private Pattern blockPattern(String block) {
        // Pattern that matches blocks regardless of indentation
        return Pattern.compile(
                "^\\s*" + Pattern.quote(block) + "\\s*\\{((?:[^{}]|\\{[^}]*\\})*)\\}",
                Pattern.MULTILINE | Pattern.DOTALL);
    }

    private String extractBlockContent(String searchContent) {
        // Strip all leading whitespace from the search content first
        String normalized = normalizeIndentation(searchContent);
        Matcher matcher = blockPattern(blockName).matcher(normalized);
        if (!matcher.find()) {
            return "";
        }
        // Extract and normalize the block content
        String blockContent = matcher.group(1);
        return normalizeIndentation(blockContent).trim();
    }

    private String updateBlock(String containerContent, String newBlockContent) {
        String normalized = normalizeIndentation(containerContent);
        Matcher matcher = blockPattern(blockName).matcher(normalized);

        if (matcher.find()) {
            // Block exists - replace it
            String newBlock = createBlock(newBlockContent);
            return normalized.substring(0, matcher.start()) + newBlock + normalized.substring(matcher.end());
        } else {
            // Block doesn't exist - insert at correct position
            String newBlock = createBlock(newBlockContent);
            int insertionPoint = findInsertionPoint(normalized);

            // Only add blank lines between top-level blocks (not nested blocks)
            boolean isTopLevel = parentSection.isEmpty();

            if (normalized.isEmpty()) {
                return newBlock;
            } else if (insertionPoint == 0) {
                // Insert at beginning
                String separator = isTopLevel ? "\n\n" : "\n";
                return newBlock + separator + normalized;
            } else if (insertionPoint >= normalized.length()) {
                // Insert at end
                String separator = isTopLevel ? "\n\n" : "\n";
                return normalized + separator + newBlock;
            } else {
                // Insert in middle - need blank lines on both sides for top-level
                if (isTopLevel) {
                    return normalized.substring(0, insertionPoint) + "\n\n" + newBlock + "\n\n"
                            + normalized.substring(insertionPoint);
                } else {
                    return normalized.substring(0, insertionPoint) + "\n" + newBlock + "\n"
                            + normalized.substring(insertionPoint);
                }
            }
        }
    }

    private String createBlock(String content) {
        if (content.trim().isEmpty()) {
            return blockName + " {\n}";
        }
        return blockName + " {\n" + content + "\n}";
    }

    private int findInsertionPoint(String containerContent) {
        // Use nested ordering when we have a parent section, otherwise use top-level ordering
        List<String> sections = parentSection.isPresent() ? NESTED_SECTIONS : ALL_SECTIONS;
        int priority = sections.indexOf(blockName);

        // If section not found in the list, append at end
        if (priority == -1) {
            return containerContent.length();
        }

        // Find last section that should come before this one
        return IntStream.range(0, priority)
                .mapToObj(i -> sections.get(priority - 1 - i))
                .map(section -> blockPattern(section).matcher(containerContent))
                .filter(Matcher::find)
                .findFirst()
                .map(Matcher::end)
                // Find first section that should come after this one
                .orElseGet(() -> IntStream.range(priority + 1, sections.size())
                        .mapToObj(sections::get)
                        .map(section -> blockPattern(section).matcher(containerContent))
                        .filter(Matcher::find)
                        .findFirst()
                        .map(Matcher::start)
                        .orElseGet(containerContent::length));
    }

    /**
     * Removes all leading whitespace from each line while preserving blank lines.
     */
    private String normalizeIndentation(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        String[] lines = content.split("\n", -1);
        StringBuilder normalized = new StringBuilder();
        boolean lastWasEmpty = false;

        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();

            if (trimmed.isEmpty()) {
                // Preserve single blank lines, collapse multiple
                if (!lastWasEmpty && i > 0 && i < lines.length - 1) {
                    normalized.append("\n");
                }
                lastWasEmpty = true;
            } else {
                normalized.append(trimmed).append("\n");
                lastWasEmpty = false;
            }
        }

        // Remove trailing newline
        String result = normalized.toString();
        if (result.endsWith("\n")) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    /**
     * Formats Gradle content by applying proper indentation based on brace nesting.
     * This is called only when writing to the file system (at the root level).
     */
    private String formatGradleContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }

        StringBuilder formatted = new StringBuilder();
        String[] lines = content.split("\n");
        int depth = 0;
        boolean lastLineWasEmpty = false;

        for (String line : lines) {
            String trimmed = line.trim();

            // Handle empty lines - preserve single empty lines, collapse multiple
            if (trimmed.isEmpty()) {
                if (!lastLineWasEmpty && formatted.length() > 0) {
                    formatted.append("\n");
                    lastLineWasEmpty = true;
                }
                continue;
            }
            lastLineWasEmpty = false;

            // Decrease depth before line if it starts with }
            if (trimmed.startsWith("}")) {
                depth = Math.max(0, depth - 1);
            }

            // Add indentation (4 spaces per level)
            if (depth > 0) {
                formatted.append("    ".repeat(depth));
            }
            formatted.append(trimmed);
            formatted.append("\n");

            // Increase depth after line if it ends with {
            if (trimmed.endsWith("{") && !trimmed.startsWith("}")) {
                depth++;
            }
        }

        // Remove trailing newline if present
        String result = formatted.toString();
        if (result.endsWith("\n")) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    protected final T getGradleFile() {
        return gradleFile;
    }
}
