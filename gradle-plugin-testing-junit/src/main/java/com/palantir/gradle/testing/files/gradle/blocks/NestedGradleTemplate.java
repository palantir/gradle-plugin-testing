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

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Template for nested blocks within Gradle files (e.g., buildscript, configurations.all).
 * Unlike top-level templates, nested templates work with a specific set of child block names.
 */
public final class NestedGradleTemplate implements GradleFileTemplate {
    private final ImmutableList<String> blockNames;

    NestedGradleTemplate(List<String> blockNames) {
        this.blockNames = ImmutableList.copyOf(blockNames);
    }

    @Override
    public String render(GradleFileState state) {
        String sections = Stream.concat(
                        blockNames.stream().map(blockName -> renderBlock(blockName, state)),
                        Stream.of(state.unstructuredContent()))
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.joining("\n\n"));

        return sections.isEmpty() || sections.endsWith("\n") ? sections : sections + "\n";
    }

    @Override
    public GradleFileState parse(String content) {
        return parseBlocks(content, blockNames);
    }

    @Override
    public ImmutableList<String> blockNames() {
        return blockNames;
    }
}
