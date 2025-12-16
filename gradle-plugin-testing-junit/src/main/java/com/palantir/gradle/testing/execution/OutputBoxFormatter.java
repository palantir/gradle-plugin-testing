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

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;

/**
 * Formats output text within a colored box border for improved visual distinction.
 * Useful for displaying inner Gradle build output within test execution logs.
 */
public final class OutputBoxFormatter {

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

    private OutputBoxFormatter() {
        // utility class
    }

    /**
     * Formats the given content in a green-bordered box with the specified title.
     *
     * @param title the title to display in the header
     * @param content the content to box
     * @return the formatted output with green borders
     */
    public static String formatSuccess(String title, String content) {
        return format(title, content, Color.GREEN);
    }

    /**
     * Formats the given content in a red-bordered box with the specified title.
     *
     * @param title the title to display in the header
     * @param content the content to box
     * @return the formatted output with red borders
     */
    public static String formatFailure(String title, String content) {
        return format(title, content, Color.RED);
    }

    private static String format(String title, String content, Color borderColor) {
        java.util.List<String> lines = content.lines().toList();

        // Calculate the width based on content, but with reasonable bounds
        int contentWidth = calculateWidth(title, lines);

        Ansi ansi = Ansi.ansi();

        // Top border with title
        ansi.fg(borderColor);
        ansi.a(TOP_LEFT);
        String header = " " + title + " ";
        ansi.a(header);
        ansi.a(repeatChar(HORIZONTAL, contentWidth - header.length()));
        ansi.a(TOP_RIGHT);
        ansi.reset();
        ansi.a('\n');

        // Separator line
        ansi.fg(borderColor);
        ansi.a(LEFT_T);
        ansi.a(repeatChar(HORIZONTAL, contentWidth));
        ansi.a(RIGHT_T);
        ansi.reset();
        ansi.a('\n');

        // Content lines - no truncation, pad to box width
        for (String line : lines) {
            ansi.fg(borderColor);
            ansi.a(VERTICAL);
            ansi.reset();
            ansi.a(' '); // left padding

            ansi.fgBright(Color.WHITE);
            ansi.a(line);
            ansi.reset();
            // Pad to match box width
            ansi.a(repeatChar(' ', contentWidth - (2 * PADDING) - line.length()));

            ansi.a(' '); // right padding
            ansi.fg(borderColor);
            ansi.a(VERTICAL);
            ansi.reset();
            ansi.a('\n');
        }

        // Bottom border
        ansi.fg(borderColor);
        ansi.a(BOTTOM_LEFT);
        ansi.a(repeatChar(HORIZONTAL, contentWidth));
        ansi.a(BOTTOM_RIGHT);
        ansi.reset();

        return ansi.toString();
    }

    private static int calculateWidth(String title, java.util.List<String> lines) {
        // Find the longest line
        int maxLineLength = 0;
        for (String line : lines) {
            if (line.length() > maxLineLength) {
                maxLineLength = line.length();
            }
        }

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
        for (int i = 0; i < count; i++) {
            chars[i] = ch;
        }
        return new String(chars);
    }
}
