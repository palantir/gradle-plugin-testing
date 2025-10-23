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
import java.util.Arrays;
import java.util.stream.Collectors;
import org.intellij.lang.annotations.Language;

public interface GradleFile extends ProjectFile<GradleFile> {
    @Override
    default GradleFile overwrite(@Language("Gradle") String text) {
        return ProjectFile.super.overwrite(text);
    }

    @Override
    default GradleFile append(@Language("Gradle") String text) {
        return ProjectFile.super.append(text);
    }

    @Override
    default GradleFile appendLine(@Language("Gradle") String line) {
        return ProjectFile.super.appendLine(line);
    }

    @Override
    default GradleFile prepend(@Language("Gradle") String text) {
        return ProjectFile.super.prepend(text);
    }

    @Override
    default GradleFile prependLine(@Language("Gradle") String line) {
        return ProjectFile.super.prependLine(line);
    }

    /**
     * Adds dependencies to the dependencies block using the 'implementation' configuration.
     * Example: {@code buildGradle().addDependencies("com.google.guava:guava:31.1-jre")}
     */
    default GradleFile addDependencies(String... dependencies) {
        return addDependency("implementation", dependencies);
    }

    /**
     * Adds a dependency to the dependencies block with a specific configuration (string-based).
     * Example: {@code buildGradle().addDependency("testImplementation", "junit:junit:4.13.2")}
     *
     * @param configuration the Gradle configuration (e.g., "implementation", "testImplementation", "api")
     * @param dependencies the dependency coordinates
     */
    default GradleFile addDependency(String configuration, String... dependencies) {
        if (dependencies.length == 0) {
            return this;
        }

        return appendLine(Arrays.stream(dependencies)
                .map(dep -> "    " + configuration + " '" + dep + "'")
                .collect(Collectors.joining("\n", "dependencies {\n", "\n}")));
    }
}
