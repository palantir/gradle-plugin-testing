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

package com.palantir.gradle.testing.project;

import com.google.common.base.Preconditions;
import com.palantir.gradle.testing.files.BuildDir;
import com.palantir.gradle.testing.files.Directory;
import com.palantir.gradle.testing.files.GradleSourceSet;
import com.palantir.gradle.testing.files.gradle.GradleFile;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public interface GradleProject extends Directory {
    @Override
    Path path();

    RootProject rootProject();

    default SubProject subproject(String name) {
        Path subprojectDir = createValidatedDirectory(path(), name, "Subproject");

        String subprojectPath =
                rootProject().path().relativize(subprojectDir).toString().replace('/', ':');

        rootProject().settingsGradle().include(subprojectPath);

        return new SubProject(subprojectDir, rootProject());
    }

    private static Path createValidatedDirectory(Path parent, String name, String type) {
        Preconditions.checkArgument(
                !name.contains(":"), "%s names must not contain colons (project name was %s)", type, name);
        Preconditions.checkArgument(
                !name.contains("/"), "%s names must not contain slashes (project name was %s)", type, name);

        Path dir = parent.resolve(name);

        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return dir;
    }

    default GradleFile buildGradle() {
        return gradleFile("build.gradle");
    }

    default GradleSourceSet mainSourceSet() {
        return sourceSet("main");
    }

    default GradleSourceSet testSourceSet() {
        return sourceSet("test");
    }

    default GradleSourceSet sourceSet(String sourceSetName) {
        return new GradleSourceSet(path().resolve("src").resolve(sourceSetName));
    }

    default BuildDir buildDir() {
        return new BuildDir(path().resolve("build"));
    }
}
