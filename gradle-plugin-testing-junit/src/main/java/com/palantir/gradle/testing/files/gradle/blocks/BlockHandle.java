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

import com.palantir.gradle.testing.files.ProjectFile;
import com.palantir.gradle.testing.files.gradle.GradleFile;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * A handle to a specific block within a Gradle file.
 * When edited, it parses the entire file, navigates to the block, edits it locally,
 * then re-renders and writes the entire file.
 */
public class BlockHandle implements GradleFile {
    protected final GradleFile root;
    protected final Template template;
    protected final String[] path;

    public BlockHandle(GradleFile root, Template template, String... path) {
        this.root = root;
        this.template = template;
        this.path = path;
    }

    @Override
    public Path path() {
        return root.path();
    }

    @Override
    public String text() {
        // Parse and navigate to this block, then render it
        ParsedContent parsed = template.parse(root.text());
        Block block = parsed.getBlockAt(template.blockTemplates(), path);
        return block.render();
    }

    @Override
    public BlockHandle edit(ProjectFile.FileEditor editor) {
        // 1. Parse entire file
        String fileContent = root.text();
        ParsedContent parsed = template.parse(fileContent);

        // 2. Navigate to this block
        Block block = parsed.getBlockAt(template.blockTemplates(), path);

        // 3. Edit the block using the editor
        Block updatedBlock = block.edit(editor::edit);

        // 4. Update the parsed content tree
        ParsedContent updated = parsed.withBlockAt(template.blockTemplates(), path, updatedBlock);

        // 5. Render entire file and write
        String rendered = template.render(updated);
        root.overwrite(rendered);

        return this;
    }

    @Override
    public BlockHandle append(String text) {
        return edit(existing -> {
            if (existing.isEmpty()) {
                return text;
            }
            return existing + "\n" + text;
        });
    }

    @Override
    public BlockHandle prepend(String text) {
        return edit(existing -> {
            if (existing.isEmpty()) {
                return text;
            }
            // If text already ends with newline, don't add another one
            if (text.endsWith("\n")) {
                return text + existing;
            }
            return text + "\n" + existing;
        });
    }

    @Override
    public BlockHandle overwrite(String text) {
        return edit(_existing -> text);
    }
}
