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

import com.palantir.gradle.testing.files.gradle.GradleFile;

/**
 * Represents the buildscript block in Gradle files with subsections for repositories and dependencies.
 * Can be used in both build.gradle and settings.gradle files.
 */
public final class BuildScriptSection<T extends GradleFile> extends GradleSection<T> {
    public BuildScriptSection(T gradleFile) {
        super(gradleFile, "buildscript");
    }

    public GradleSection<T> repositories() {
        return new GradleSection<>(getGradleFile(), this, "repositories");
    }

    public GradleSection<T> dependencies() {
        return new GradleSection<>(getGradleFile(), this, "dependencies");
    }
}
