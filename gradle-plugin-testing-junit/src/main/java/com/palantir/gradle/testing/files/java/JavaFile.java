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

package com.palantir.gradle.testing.files.java;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import com.google.errorprone.annotations.RestrictedApi;
import com.palantir.gradle.testing.RestrictedCreation;
import com.palantir.gradle.testing.files.ProjectFile;
import java.nio.file.Path;
import org.intellij.lang.annotations.Language;

public record JavaFile(Path path) implements ProjectFile<JavaFile> {
    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public JavaFile {}

    @Override
    @FormatMethod
    public JavaFile overwrite(@Language("Java") @FormatString String text, Object... args) {
        return ProjectFile.super.overwrite(text, args);
    }

    @Override
    @FormatMethod
    public JavaFile append(@Language("Java") @FormatString String text, Object... args) {
        return ProjectFile.super.append(text, args);
    }

    @Override
    @FormatMethod
    public JavaFile appendLine(@Language("Java") @FormatString String line, Object... args) {
        return ProjectFile.super.appendLine(line, args);
    }

    @Override
    @FormatMethod
    public JavaFile prepend(@Language("Java") @FormatString String text, Object... args) {
        return ProjectFile.super.prepend(text, args);
    }

    @Override
    @FormatMethod
    public JavaFile prependLine(@Language("Java") @FormatString String line, Object... args) {
        return ProjectFile.super.prependLine(line, args);
    }
}
