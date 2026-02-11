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

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import com.google.errorprone.annotations.RestrictedApi;
import com.palantir.gradle.testing.RestrictedCreation;
import com.palantir.gradle.testing.files.ProjectFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.intellij.lang.annotations.Language;
import org.intellij.lang.annotations.PrintFormat;

public record PropertiesFile(Path path) implements ProjectFile<PropertiesFile> {
    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public PropertiesFile {}

    /**
     * Sets the property key to the {@code value} in the file.
     */
    public PropertiesFile appendProperty(String key, String value) {
        String originalText = Files.exists(path()) ? text() : "";
        Matcher matcher = getPropertyMatcher(originalText, key);
        if (!matcher.find()) {
            return appendLine("%s=%s", key, value);
        }
        String existingValue = matcher.group(1);
        return edit(text -> text.replaceFirst(
                String.format("(?m)^%s=%s$", Pattern.quote(key), Pattern.quote(existingValue)),
                String.format("%s=%s", key, value)));
    }

    private Matcher getPropertyMatcher(String text, String key) {
        Pattern pattern = Pattern.compile(String.format("(?m)^%s=(.*)$", Pattern.quote(key)));
        return pattern.matcher(text);
    }

    @Override
    @FormatMethod
    public PropertiesFile overwrite(@Language("Properties") @PrintFormat @FormatString String text, Object... args) {
        return ProjectFile.super.overwrite(text, args);
    }

    @Override
    public PropertiesFile overwrite(@Language("Properties") String text) {
        return ProjectFile.super.overwrite(text);
    }

    @Override
    @FormatMethod
    public PropertiesFile append(@Language("Properties") @PrintFormat @FormatString String text, Object... args) {
        return ProjectFile.super.append(text, args);
    }

    @Override
    public PropertiesFile append(@Language("Properties") String text) {
        return ProjectFile.super.append(text);
    }

    @Override
    @FormatMethod
    public PropertiesFile appendLine(@Language("Properties") @PrintFormat @FormatString String line, Object... args) {
        return ProjectFile.super.appendLine(line, args);
    }

    @Override
    public PropertiesFile appendLine(@Language("Properties") String line) {
        return ProjectFile.super.appendLine(line);
    }

    @Override
    @FormatMethod
    public PropertiesFile prepend(@Language("Properties") @PrintFormat @FormatString String text, Object... args) {
        return ProjectFile.super.prepend(text, args);
    }

    @Override
    public PropertiesFile prepend(@Language("Properties") String text) {
        return ProjectFile.super.prepend(text);
    }

    @Override
    @FormatMethod
    public PropertiesFile prependLine(@Language("Properties") @PrintFormat @FormatString String line, Object... args) {
        return ProjectFile.super.prependLine(line, args);
    }

    @Override
    public PropertiesFile prependLine(@Language("Properties") String line) {
        return ProjectFile.super.prependLine(line);
    }
}
