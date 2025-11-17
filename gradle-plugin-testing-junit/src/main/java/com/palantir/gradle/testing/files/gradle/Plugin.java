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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A single plugin declaration with ID and optional apply setting.
 * <p>
 * Example: {@code new Plugin("java", Optional.of(false))}
 *
 * @see Plugins#add(String)
 */
record Plugin(String id, Optional<Boolean> apply) {
    // Matches: id 'plugin-name' or id 'plugin-name' apply false
    // Captures: (1) plugin ID, (2) optional apply value (true/false)
    private static final Pattern PLUGIN_PATTERN = Pattern.compile(
            "id" // literal "id"
                    + "\\s+" // required whitespace
                    + "'([^']+)'" // capture group 1: plugin ID in single quotes
                    + "(?:" // non-capturing group for optional apply clause:
                    + "\\s+" //   whitespace
                    + "apply" //   literal "apply"
                    + "\\s*" //   optional whitespace
                    + "\\(?" //   optional opening paren
                    + "\\s*" //   optional whitespace
                    + "(true|false)" //   capture group 2: true or false
                    + "\\s*" //   optional whitespace
                    + "\\)?" //   optional closing paren
                    + ")?"); // end optional group

    /**
     * Parses {@code "id 'java'"} or {@code "id 'plugin' apply false"} into a Plugin.
     */
    Plugin(String declaration) {
        this(parseId(declaration), parseApply(declaration));
    }

    private static String parseId(String declaration) {
        Matcher matcher = PLUGIN_PATTERN.matcher(declaration.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid plugin declaration: " + declaration);
        }
        return matcher.group(1);
    }

    private static Optional<Boolean> parseApply(String declaration) {
        Matcher matcher = PLUGIN_PATTERN.matcher(declaration.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid plugin declaration: " + declaration);
        }
        return Optional.ofNullable(matcher.group(2)).map(Boolean::valueOf);
    }

    /**
     * Returns {@code "id 'java'"} or {@code "id 'plugin' apply false"}.
     */
    String toDeclaration() {
        StringBuilder sb = new StringBuilder("id '").append(id).append("'");
        apply.ifPresent(a -> sb.append(" apply ").append(a));
        return sb.toString();
    }
}
