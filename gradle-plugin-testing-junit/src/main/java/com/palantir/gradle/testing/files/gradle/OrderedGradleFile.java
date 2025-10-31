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

import com.palantir.gradle.testing.files.ProjectFile;
import com.palantir.gradle.testing.files.gradle.blocks.GradleFileState;
import com.palantir.gradle.testing.files.gradle.blocks.GradleFileTemplate;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Base class for Gradle files with structured blocks that follow canonical ordering.
 * Provides template-based parsing and rendering to maintain consistent block order.
 */
public abstract class OrderedGradleFile implements GradleFile {
    private final Path path;

    protected OrderedGradleFile(Path path) {
        this.path = path;
    }

    @Override
    public final Path path() {
        return path;
    }

    @Override
    public String text() {
        if (!Files.exists(path())) {
            return "";
        }
        return GradleFile.super.text();
    }

    @Override
    public GradleFile edit(ProjectFile.FileEditor editor) {
        GradleFileState currentState = parseState();
        String editedContent = editor.edit(template().render(currentState));
        GradleFileState newState = template().parse(editedContent);
        overwrite(template().render(newState));
        return this;
    }

    public final GradleFileState parseState() {
        return Files.exists(path()) ? template().parse(text()) : GradleFileState.empty();
    }

    /**
     * Returns the template used for parsing and rendering this file.
     */
    public final GradleFileTemplate template() {
        return templateInternal();
    }

    /**
     * Returns the template used for parsing and rendering this file.
     */
    protected abstract GradleFileTemplate templateInternal();
}
