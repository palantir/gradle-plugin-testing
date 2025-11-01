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

package com.palantir.gradle.testing.files.gradle.blocks;

import java.util.Map;

/**
 * Template for parsing and rendering Gradle files.
 * A template defines the structure of a file by specifying top-level blocks and their ordering.
 */
public interface Template {
    /**
     * Parse entire file content into structured blocks.
     */
    ParsedContent parse(String fileContent);

    /**
     * Render parsed content back to file string.
     */
    String render(ParsedContent content);

    /**
     * Get the block templates (structure definitions) for this template.
     * These are used to create empty blocks when they don't exist yet.
     */
    Map<String, Block> blockTemplates();
}
