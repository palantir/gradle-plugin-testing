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
 */
public class GradleSection<T extends GradleFile> implements GradleFile {
    // Master list of all sections in canonical order - used for relative ordering
    protected static final List<String> ALL_SECTIONS = List.of(
            "pluginManagement", "buildscript", "plugins", "allprojects", "subprojects", "repositories", "dependencies");

    private final T gradleFile;
    private final String blockName;
    private final Optional<GradleSection<T>> parentSection;

    protected GradleSection(T gradleFile, String blockName) {
        this(gradleFile, Optional.empty(), blockName);
    }

    protected GradleSection(T gradleFile, GradleSection<T> parentSection, String blockName) {
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
                    gradleFile.overwrite(updateBlock(fileContent, editor.edit(extractBlockContent(fileContent))));
                });
        return this;
    }

    @Override
    public final GradleSection<T> append(String content) {
        return edit(existing -> {
            if (existing.isEmpty()) {
                return content;
            }
            return existing + "\n" + getIndent() + content;
        });
    }

    @Override
    public final GradleSection<T> prepend(String content) {
        return edit(existing -> {
            if (existing.isEmpty()) {
                return content;
            }
            return content + "\n" + getIndent() + existing;
        });
    }

    @Override
    public final GradleSection<T> overwrite(String content) {
        return edit(_existing -> content);
    }

    private int getNestingDepth() {
        return parentSection.map(parent -> 1 + parent.getNestingDepth()).orElse(0);
    }

    private String getIndent() {
        // 4 spaces per nesting level, starting at depth 1 (inside any block)
        return "    ".repeat(getNestingDepth() + 1);
    }

    private String getBlockIndent() {
        // Indent for the block itself (not its contents)
        return "    ".repeat(getNestingDepth());
    }

    private Pattern blockPattern(String block) {
        return Pattern.compile(block + "\\s*\\{([^}]*(?:\\{[^}]*\\}[^}]*)*)\\}", Pattern.DOTALL);
    }

    private String extractBlockContent(String searchContent) {
        Matcher matcher = blockPattern(blockName).matcher(searchContent);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private String updateBlock(String containerContent, String newBlockContent) {
        Matcher matcher = blockPattern(blockName).matcher(containerContent);

        if (matcher.find()) {
            // Block exists - replace it
            String formattedContent = formatBlockContent(newBlockContent);
            String newBlock = blockName + " {" + formattedContent + "}";
            return containerContent.substring(0, matcher.start())
                    + newBlock
                    + containerContent.substring(matcher.end());
        } else {
            // Block doesn't exist - insert at correct position
            String newBlock = createBlock(newBlockContent);
            int insertionPoint = findInsertionPoint(containerContent);

            if (containerContent.isEmpty() || insertionPoint == 0) {
                return newBlock + (containerContent.isEmpty() ? "" : "\n" + containerContent);
            } else {
                return containerContent.substring(0, insertionPoint) + "\n" + newBlock
                        + containerContent.substring(insertionPoint);
            }
        }
    }

    private String formatBlockContent(String content) {
        if (content.trim().isEmpty()) {
            return getNestingDepth() > 0 ? "\n" + getBlockIndent() : "\n";
        }
        return "\n" + getIndent() + content + "\n" + getBlockIndent();
    }

    private String createBlock(String content) {
        return blockName + " {\n" + getIndent() + content + "\n" + getBlockIndent() + "}";
    }

    private int findInsertionPoint(String containerContent) {
        int priority = ALL_SECTIONS.indexOf(blockName);

        // Find last section that should come before this one
        return IntStream.range(0, priority)
                .mapToObj(i -> ALL_SECTIONS.get(priority - 1 - i))
                .map(section -> blockPattern(section).matcher(containerContent))
                .filter(Matcher::find)
                .findFirst()
                .map(Matcher::end)
                // Find first section that should come after this one
                .orElseGet(() -> IntStream.range(priority + 1, ALL_SECTIONS.size())
                        .mapToObj(ALL_SECTIONS::get)
                        .map(section -> blockPattern(section).matcher(containerContent))
                        .filter(Matcher::find)
                        .findFirst()
                        .map(Matcher::start)
                        .orElseGet(containerContent::length));
    }

    protected final T getGradleFile() {
        return gradleFile;
    }

    protected final String getBlockName() {
        return blockName;
    }
}
