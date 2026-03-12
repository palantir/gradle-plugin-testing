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
import com.palantir.gradle.testing.files.gradle.SettingsGradleFile;
import com.palantir.gradle.testing.files.properties.PropertiesFile;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A Gradle project that is the root of a build, having its own {@code settings.gradle} and
 * {@code gradle.properties}. Both the top-level project and included builds are root projects.
 */
public interface RootProject extends GradleProject {
    @Override
    default RootProject rootProject() {
        return this;
    }

    default SettingsGradleFile settingsGradle() {
        return new SettingsGradleFile(path().resolve("settings.gradle"));
    }

    default PropertiesFile gradlePropertiesFile() {
        return propertiesFile("gradle.properties");
    }

    default IncludedBuild includedBuild(String name) {
        Preconditions.checkArgument(
                !name.contains(":"), "Included build names must not contain colons (project name was %s)", name);
        Preconditions.checkArgument(
                !name.contains("/"), "Included build names must not contain slashes (project name was %s)", name);

        Path includedBuildDir = path().resolve(name);

        try {
            Files.createDirectories(includedBuildDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        settingsGradle().includeBuild(name);

        return new IncludedBuild(name, includedBuildDir);
    }
}
