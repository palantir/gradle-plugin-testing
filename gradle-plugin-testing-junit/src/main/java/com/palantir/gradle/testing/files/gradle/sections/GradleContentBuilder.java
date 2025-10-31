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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Immutable template-based builder for Gradle file content.
 * Parses known sections, allows modification, and rebuilds using a canonical template order.
 */
public final class GradleContentBuilder {
    private static final List<String> TOP_LEVEL_SECTIONS = ImmutableList.of(
            "pluginManagement", "buildscript", "plugins", "allprojects", "subprojects", "repositories", "dependencies");

    private static final List<String> NESTED_SECTIONS = ImmutableList.of("repositories", "dependencies", "plugins");

    private final ImmutableList<String> sectionOrder;
    private final ImmutableMap<String, String> sections;
    private final String unrecognizedContent;

    private GradleContentBuilder(
            ImmutableList<String> sectionOrder, ImmutableMap<String, String> sections, String unrecognizedContent) {
        this.sectionOrder = sectionOrder;
        this.sections = sections;
        this.unrecognizedContent = unrecognizedContent;
    }

    /**
     * Parse content using top-level section ordering.
     */
    public static GradleContentBuilder parseTopLevel(String content) {
        return parse(content, TOP_LEVEL_SECTIONS);
    }

    /**
     * Parse content using nested section ordering.
     */
    public static GradleContentBuilder parseNested(String content) {
        return parse(content, NESTED_SECTIONS);
    }

    private static GradleContentBuilder parse(String content, List<String> sectionOrder) {
        if (content == null || content.trim().isEmpty()) {
            return new GradleContentBuilder(ImmutableList.copyOf(sectionOrder), ImmutableMap.of(), "");
        }

        // Extract all known sections, tracking what content remains
        String[] remaining = {content};
        ImmutableMap.Builder<String, String> sectionsBuilder = ImmutableMap.builder();

        sectionOrder.forEach(sectionName -> {
            Pattern pattern = blockPattern(sectionName);
            Matcher matcher = pattern.matcher(remaining[0]);

            if (matcher.find()) {
                String blockContent = matcher.group(1).trim();
                sectionsBuilder.put(sectionName, blockContent);
                // Remove this section from remaining content
                remaining[0] = remaining[0].substring(0, matcher.start())
                        + remaining[0].substring(matcher.end());
            }
        });

        // Normalize remaining content: collapse multiple consecutive blank lines to single blank line
        String normalizedRemaining = remaining[0].trim().replaceAll("\n{2,}", "\n");

        return new GradleContentBuilder(
                ImmutableList.copyOf(sectionOrder), sectionsBuilder.buildOrThrow(), normalizedRemaining);
    }

    private static Pattern blockPattern(String blockName) {
        // Pattern that matches blocks with their content, handling nested braces
        return Pattern.compile(
                "^\\s*" + Pattern.quote(blockName) + "\\s*\\{([^{}]*(?:\\{[^{}]*\\}[^{}]*)*)\\}",
                Pattern.MULTILINE | Pattern.DOTALL);
    }

    /**
     * Get the content of a section (empty string if not present).
     */
    String getSection(String sectionName) {
        return sections.getOrDefault(sectionName, "");
    }

    /**
     * Returns a new builder with the section content updated.
     * Immutable operation - returns a new instance.
     */
    GradleContentBuilder withSection(String sectionName, String content) {
        ImmutableMap.Builder<String, String> newSections = ImmutableMap.builder();

        // Copy all existing sections except the one being updated
        sections.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(sectionName))
                .forEach(newSections::put);

        // Add the new section if not empty
        if (!content.trim().isEmpty()) {
            newSections.put(sectionName, content.trim());
        }

        return new GradleContentBuilder(sectionOrder, newSections.buildOrThrow(), unrecognizedContent);
    }

    /**
     * Returns a new builder with the unrecognized content updated.
     * Immutable operation - returns a new instance.
     */
    GradleContentBuilder withUnrecognizedContent(String content) {
        return new GradleContentBuilder(sectionOrder, sections, content);
    }

    /**
     * Build the final content using template-based string formatting.
     * Collects non-empty formatted sections and joins them with appropriate spacing.
     */
    String build(boolean isTopLevel) {
        String separator = isTopLevel ? "\n\n" : "\n";

        // Collect non-empty formatted sections
        List<String> formattedSections = sectionOrder.stream()
                .map(this::formatSection)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        // Join sections with separator
        String sectionsText = String.join(separator, formattedSections);

        // Append unrecognized content
        if (!unrecognizedContent.isEmpty()) {
            if (!sectionsText.isEmpty()) {
                return sectionsText + separator + unrecognizedContent;
            }
            return unrecognizedContent;
        }

        return sectionsText;
    }

    /**
     * Format a single section as "sectionName { content }" with proper indentation.
     * Returns empty string if section has no content.
     */
    private String formatSection(String sectionName) {
        String content = sections.get(sectionName);
        if (content == null || content.trim().isEmpty()) {
            return "";
        }

        String indentedContent = indentContent(content);
        return String.format("%s {\n%s\n}", sectionName, indentedContent);
    }

    private String indentContent(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        // Track indentation level across lines
        int[] currentLevel = {1};

        List<String> lines = Splitter.on('\n').splitToList(content);
        return lines.stream()
                .map(line -> {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) {
                        return "";
                    }

                    // Decrease indentation before line if it starts with }
                    int lineLevel = currentLevel[0];
                    if (trimmed.startsWith("}")) {
                        lineLevel = Math.max(0, currentLevel[0] - 1);
                    }

                    String indented = "    ".repeat(lineLevel) + trimmed;

                    // Update level for next line
                    if (trimmed.endsWith("{") && !trimmed.startsWith("}")) {
                        currentLevel[0]++;
                    } else if (trimmed.equals("}")) {
                        currentLevel[0] = Math.max(1, currentLevel[0] - 1);
                    }

                    return indented;
                })
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("\n"));
    }
}
