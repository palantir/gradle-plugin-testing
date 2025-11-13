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

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import com.palantir.gradle.testing.files.ProjectFile;
import org.intellij.lang.annotations.Language;
import org.intellij.lang.annotations.PrintFormat;

/**
 * Represents a Gradle build file (*.gradle) written in Groovy DSL.
 * <p>
 * Note: This API only supports Groovy-based Gradle files, not Kotlin DSL (*.gradle.kts).
 */
public interface GradleFile extends ProjectFile<GradleFile> {

    /**
     * Returns API for managing the {@code plugins {}} block.
     */
    default Plugins plugins() {
        return new Plugins(this);
    }

    @Override
    @FormatMethod
    default GradleFile overwrite(@Language("Gradle") @PrintFormat @FormatString String text, Object... args) {
        return ProjectFile.super.overwrite(text, args);
    }

    @Override
    default GradleFile overwrite(@Language("Gradle") String text) {
        return ProjectFile.super.overwrite(text);
    }

    @Override
    @FormatMethod
    default GradleFile append(@Language("Gradle") @PrintFormat @FormatString String text, Object... args) {
        ProjectFile.super.append(text, args);
        return ensurePluginsBlockPosition();
    }

    @Override
    default GradleFile append(@Language("Gradle") String text) {
        ProjectFile.super.append(text);
        return ensurePluginsBlockPosition();
    }

    @Override
    @FormatMethod
    default GradleFile appendLine(@Language("Gradle") @PrintFormat @FormatString String line, Object... args) {
        ProjectFile.super.appendLine(line, args);
        return ensurePluginsBlockPosition();
    }

    @Override
    default GradleFile appendLine(@Language("Gradle") String line) {
        ProjectFile.super.appendLine(line);
        return ensurePluginsBlockPosition();
    }

    @Override
    @FormatMethod
    default GradleFile prepend(@Language("Gradle") @PrintFormat @FormatString String text, Object... args) {
        ProjectFile.super.prepend(text, args);
        return ensurePluginsBlockPosition();
    }

    @Override
    default GradleFile prepend(@Language("Gradle") String text) {
        ProjectFile.super.prepend(text);
        return ensurePluginsBlockPosition();
    }

    @Override
    @FormatMethod
    default GradleFile prependLine(@Language("Gradle") @PrintFormat @FormatString String line, Object... args) {
        ProjectFile.super.prependLine(line, args);
        return ensurePluginsBlockPosition();
    }

    @Override
    default GradleFile prependLine(@Language("Gradle") String line) {
        ProjectFile.super.prependLine(line);
        return ensurePluginsBlockPosition();
    }

    /**
     * Ensures the plugins block is positioned correctly (at top or after buildscript).
     * This is called automatically after append/prepend operations to maintain proper ordering.
     */
    private GradleFile ensurePluginsBlockPosition() {
        edit(Plugins::repositionPluginsBlock);
        return this;
    }
}
