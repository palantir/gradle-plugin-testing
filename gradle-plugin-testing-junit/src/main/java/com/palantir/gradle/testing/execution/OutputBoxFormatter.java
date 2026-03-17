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

package com.palantir.gradle.testing.execution;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Formats output text within a colored box border for improved visual distinction.
 * Useful for displaying inner Gradle build output within test execution logs.
 */
public final class OutputBoxFormatter {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_BRIGHT_WHITE = "\u001B[97m";

    // Box drawing characters (Unicode)
    private static final char TOP_LEFT = '╔';
    private static final char TOP_RIGHT = '╗';
    private static final char BOTTOM_LEFT = '╚';
    private static final char BOTTOM_RIGHT = '╝';
    private static final char HORIZONTAL = '═';
    private static final char VERTICAL = '║';
    private static final char LEFT_T = '╠';
    private static final char RIGHT_T = '╣';

    private static final int PADDING = 1; // spaces on each side of content

    private OutputBoxFormatter() {}

    public static String formatSuccess(String title, String content) {
        if (System.getenv("CI") == null) {
            return format(title, content, Optional.of(ANSI_GREEN));
        } else {
            return format("SUCCESS: " + title, content, Optional.empty());
        }
    }

    public static String formatFailure(String title, String content) {
        if (System.getenv("CI") == null) {
            return format(title, content, Optional.of(ANSI_RED));
        } else {
            return format("FAILURE: " + title, content, Optional.empty());
        }
    }

    private static String format(String title, String content, Optional<String> color) {
        List<String> lines = content.lines().toList();

        // Calculate the width based on content, but with reasonable bounds
        int contentWidth = calculateWidth(title, lines);

        StringBuilder result = new StringBuilder();

        // Top border with title
        color.ifPresent(result::append);
        result.append(TOP_LEFT);
        String header = " " + title + " ";
        result.append(header);
        result.append(repeatChar(HORIZONTAL, contentWidth - header.length()));
        result.append(TOP_RIGHT);
        color.ifPresent(unused -> result.append(ANSI_RESET));
        result.append('\n');

        // Separator line
        color.ifPresent(result::append);
        result.append(LEFT_T);
        result.append(repeatChar(HORIZONTAL, contentWidth));
        result.append(RIGHT_T);
        color.ifPresent(unused -> result.append(ANSI_RESET));
        result.append('\n');

        // Content lines - no truncation, pad to box width
        for (String line : lines) {
            color.ifPresent(result::append);
            result.append(VERTICAL);
            color.ifPresent(unused -> result.append(ANSI_RESET));
            result.append(' '); // left padding

            color.ifPresent(result::append);
            color.ifPresent(unused -> result.append(ANSI_BRIGHT_WHITE));
            result.append(line);
            color.ifPresent(unused -> result.append(ANSI_RESET));
            // Pad to match box width
            result.append(repeatChar(' ', contentWidth - (2 * PADDING) - line.length()));

            result.append(' '); // right padding
            color.ifPresent(result::append);
            result.append(VERTICAL);
            color.ifPresent(unused -> result.append(ANSI_RESET));
            result.append('\n');
        }

        // Bottom border
        color.ifPresent(result::append);
        result.append(BOTTOM_LEFT);
        result.append(repeatChar(HORIZONTAL, contentWidth));
        result.append(BOTTOM_RIGHT);
        color.ifPresent(unused -> result.append(ANSI_RESET));

        return result.toString();
    }

    private static int calculateWidth(String title, List<String> lines) {
        // Find the longest line
        int maxLineLength = lines.stream().mapToInt(String::length).max().orElse(0);

        // Account for title with checkmark/cross prefix
        int titleLength = title.length() + 4; // " ✓ " or " ✗ " + title + " "

        // Use the larger of title or content, with padding
        return Math.max(titleLength, maxLineLength + (2 * PADDING));
    }

    private static String repeatChar(char ch, int count) {
        if (count <= 0) {
            return "";
        }
        char[] chars = new char[count];
        Arrays.fill(chars, ch);
        return new String(chars);
    }
}
