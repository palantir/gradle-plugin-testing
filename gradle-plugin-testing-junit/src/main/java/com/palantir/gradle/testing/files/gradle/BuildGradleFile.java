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

import com.google.errorprone.annotations.RestrictedApi;
import com.palantir.gradle.testing.RestrictedCreation;
import com.palantir.gradle.testing.files.gradle.sections.BuildScriptSection;
import com.palantir.gradle.testing.files.gradle.sections.GenericSection;
import java.nio.file.Path;

public record BuildGradleFile(Path path) implements GradleFile {
    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public BuildGradleFile {}

    public BuildScriptSection<BuildGradleFile> buildscript() {
        return new BuildScriptSection<>(this);
    }

    public GenericSection<BuildGradleFile> plugins() {
        return new GenericSection<>(this, "plugins");
    }

    public GenericSection<BuildGradleFile> repositories() {
        return new GenericSection<>(this, "repositories");
    }

    public GenericSection<BuildGradleFile> dependencies() {
        return new GenericSection<>(this, "dependencies");
    }

    public GenericSection<BuildGradleFile> allprojects() {
        return new GenericSection<>(this, "allprojects");
    }

    public GenericSection<BuildGradleFile> subprojects() {
        return new GenericSection<>(this, "subprojects");
    }
}
