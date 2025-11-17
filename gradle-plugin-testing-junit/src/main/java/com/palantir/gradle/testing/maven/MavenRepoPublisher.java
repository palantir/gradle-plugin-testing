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

package com.palantir.gradle.testing.maven;

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.execution.GradleVersion;
import com.palantir.gradle.testing.project.RootProject;
import com.palantir.gradle.testing.project.SubProject;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Internal class that manages the Gradle project structure for publishing modules.
 * This creates an independent Gradle project with subprojects for each module.
 */
final class MavenRepoPublisher {
    private final RootProject rootProject;
    private final GradleInvoker gradleInvoker;
    private final Path mavenRepoUrl;

    MavenRepoPublisher(Path projectRoot, Path mavenRepoUrl, GradleVersion gradleVersion) {
        this.rootProject = new RootProject(projectRoot);
        this.gradleInvoker = new GradleInvoker(projectRoot, gradleVersion);
        this.mavenRepoUrl = mavenRepoUrl;

        rootProject.settingsGradle().rootProjectName("maven-repo-publisher");
    }

    void publish(List<MavenArtifact> artifacts) {
        artifacts.forEach(artifact -> {
            createSubproject(artifact, subprojectName(artifact.coordinate()));
        });

        gradleInvoker
                .withArgs("publishMavenPublicationToMavenRepository", "--quiet")
                .buildsSuccessfully();
    }

    private void createSubproject(MavenArtifact artifact, String subprojectName) {
        SubProject subproject = rootProject.subproject(subprojectName);
        MavenCoordinate coordinate = artifact.coordinate();

        String dependenciesBlock = artifact.dependencies().isEmpty()
                ? ""
                : """
                dependencies {
                    %s
                }
                """.formatted(artifact.dependencies().stream()
                        .map(dep -> "    api '%s'".formatted(dep.asString()))
                        .collect(Collectors.joining("\n")));

        subproject
                .buildGradle()
                .overwrite(
                        """
                        group = '%s'
                        version = '%s'

                        repositories {
                            maven {
                                url = '%s'
                            }
                            mavenCentral()
                        }

                        %s
                        publishing {
                            repositories {
                                maven {
                                    url = '%s'
                                }
                            }
                            publications {
                                maven(MavenPublication) {
                                    artifactId = '%s'
                                    from components.java
                                }
                            }
                        }
                        """,
                        coordinate.group(),
                        coordinate.version(),
                        mavenRepoUrl,
                        dependenciesBlock,
                        mavenRepoUrl,
                        coordinate.artifact());
        subproject.buildGradle().plugins().add("java-library").add("maven-publish");
    }

    private static String subprojectName(MavenCoordinate coordinate) {
        return coordinate.group() + "_" + coordinate.artifact() + "_"
                + coordinate.version().replace('.', '_');
    }
}
