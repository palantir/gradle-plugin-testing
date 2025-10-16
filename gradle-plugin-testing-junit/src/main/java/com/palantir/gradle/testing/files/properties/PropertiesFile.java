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

package com.palantir.gradle.testing.files.properties;

import com.google.errorprone.annotations.RestrictedApi;
import com.palantir.gradle.testing.RestrictedCreation;
import com.palantir.gradle.testing.files.ProjectFile;
import java.nio.file.Path;
import org.intellij.lang.annotations.Language;

public record PropertiesFile(Path path) implements ProjectFile<PropertiesFile> {
    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public PropertiesFile {}

    public PropertiesFile appendProperty(String key, String value) {
        return appendLine("%s=%s".formatted(key, value));
    }

    @Override
    public PropertiesFile overwrite(@Language("Properties") String text) {
        return ProjectFile.super.overwrite(text);
    }

    @Override
    public PropertiesFile append(@Language("Properties") String text) {
        return ProjectFile.super.append(text);
    }

    @Override
    public PropertiesFile appendLine(@Language("Properties") String line) {
        return ProjectFile.super.appendLine(line);
    }

    @Override
    public PropertiesFile prepend(@Language("Properties") String text) {
        return ProjectFile.super.prepend(text);
    }

    @Override
    public PropertiesFile prependLine(@Language("Properties") String line) {
        return ProjectFile.super.prependLine(line);
    }
}
