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
import com.palantir.gradle.testing.maven.MavenRepo;
import org.intellij.lang.annotations.Language;
import org.intellij.lang.annotations.PrintFormat;

public interface GradleFile extends ProjectFile<GradleFile> {
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
        return ProjectFile.super.append(text, args);
    }

    @Override
    default GradleFile append(@Language("Gradle") String text) {
        return ProjectFile.super.append(text);
    }

    @Override
    @FormatMethod
    default GradleFile appendLine(@Language("Gradle") @PrintFormat @FormatString String line, Object... args) {
        return ProjectFile.super.appendLine(line, args);
    }

    @Override
    default GradleFile appendLine(@Language("Gradle") String line) {
        return ProjectFile.super.appendLine(line);
    }

    @Override
    @FormatMethod
    default GradleFile prepend(@Language("Gradle") @PrintFormat @FormatString String text, Object... args) {
        return ProjectFile.super.prepend(text, args);
    }

    @Override
    default GradleFile prepend(@Language("Gradle") String text) {
        return ProjectFile.super.prepend(text);
    }

    @Override
    @FormatMethod
    default GradleFile prependLine(@Language("Gradle") @PrintFormat @FormatString String line, Object... args) {
        return ProjectFile.super.prependLine(line, args);
    }

    @Override
    default GradleFile prependLine(@Language("Gradle") String line) {
        return ProjectFile.super.prependLine(line);
    }

    default GradleFile withMavenRepo(MavenRepo repo) {
        return prepend("""
            buildscript {
                repositories {
                    %s
                }
            }
            """, repo.repositoryBlock()).append("""
                repositories {
                    %s
                }
                """, repo.repositoryBlock());
    }
}
