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

import com.google.errorprone.annotations.RestrictedApi;
import com.palantir.gradle.testing.RestrictedCreation;
import com.palantir.gradle.testing.files.gradle.SettingsGradleFile;
import com.palantir.gradle.testing.files.properties.PropertiesFile;
import java.nio.file.Path;

/**
 * When injected as a parameter in JUnit test methods, the parameter name will be used as the build name exactly.
 * For example, {@code IncludedBuild myLib} creates an included build named "myLib".
 * <br>
 * When injected, the included build will be registered via {@code includeBuild} in the
 * root project's settings.gradle.
 */
public final class IncludedBuild implements GradleProject {
    private final Path path;
    private final RootProject rootProject;

    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public IncludedBuild(String name, Path path) {
        this.path = path;
        this.rootProject = new RootProject(path);
        this.rootProject.settingsGradle().rootProjectName(name);
        this.rootProject.gradlePropertiesFile().setProperty("org.gradle.parallel", "true");
    }

    @Override
    public Path path() {
        return path;
    }

    @Override
    public RootProject rootProject() {
        return rootProject;
    }

    public SettingsGradleFile settingsGradle() {
        return rootProject.settingsGradle();
    }

    public PropertiesFile gradlePropertiesFile() {
        return rootProject.gradlePropertiesFile();
    }
}
