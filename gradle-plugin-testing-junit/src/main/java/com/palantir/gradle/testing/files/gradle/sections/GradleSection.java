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
import java.util.Optional;
import org.intellij.lang.annotations.Language;

/**
 * Base class for structured sections within Gradle files.
 * Implements GradleFile so all file operations work within the section's block context.
 * Uses a template-based builder approach for parsing and rendering sections.
 */
public class GradleSection<T extends GradleFile> implements GradleFile {
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
        return parentSection
                .map(parent -> {
                    GradleContentBuilder builder = GradleContentBuilder.parseNested(parent.text());
                    return builder.getSection(blockName);
                })
                .orElseGet(() -> {
                    GradleContentBuilder builder = GradleContentBuilder.parseTopLevel(gradleFile.text());
                    return builder.getSection(blockName);
                });
    }

    @Override
    public final GradleSection<T> edit(ProjectFile.FileEditor editor) {
        parentSection.ifPresentOrElse(
                parent -> parent.edit(parentContent -> {
                    GradleContentBuilder parsed = GradleContentBuilder.parseNested(parentContent);
                    return parsed.withSection(blockName, editor.edit(parsed.getSection(blockName)))
                            .build(false);
                }),
                () -> {
                    String fileContent = Files.exists(path()) ? gradleFile.text() : "";
                    GradleContentBuilder parsed = GradleContentBuilder.parseTopLevel(fileContent);
                    @Language("Gradle")
                    String newContent = parsed.withSection(blockName, editor.edit(parsed.getSection(blockName)))
                            .build(true);
                    gradleFile.overwrite(newContent);
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

    protected final T getGradleFile() {
        return gradleFile;
    }
}
