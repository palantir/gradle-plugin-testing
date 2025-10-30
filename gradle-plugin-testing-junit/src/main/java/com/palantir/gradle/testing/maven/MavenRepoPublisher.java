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

    MavenRepoPublisher(Path projectRoot, Path mavenRepoUrl, GradleVersion gradleVersion) {
        this.rootProject = new RootProject(projectRoot);
        this.gradleInvoker = new GradleInvoker(projectRoot, gradleVersion);

        rootProject.settingsGradle().rootProjectName("maven-repo-publisher");

        rootProject.buildGradle().overwrite("""
            subprojects {
                apply plugin: 'java-library'
                apply plugin: 'maven-publish'

                repositories {
                    maven {
                        url = '%s'
                    }
                    mavenCentral()
                }

                publishing {
                    repositories {
                        maven {
                            url = '%s'
                        }
                    }
                }
            }
            """, mavenRepoUrl, mavenRepoUrl);
    }

    void publish(List<MavenArtifact> artifacts) {
        artifacts.forEach(artifact -> {
            createSubproject(artifact, subprojectName(artifact.coordinate()));
            runPublish(artifact.coordinate());
        });
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
                        .map(dep -> "    api '%s:%s:%s'"
                                .formatted(dep.group(), dep.artifact(), dep.version()))
                        .collect(Collectors.joining("\n")));

        subproject.buildGradle().overwrite("""
            group = '%s'
            version = '%s'

            %s
            publishing {
                publications {
                    maven(MavenPublication) {
                        artifactId = '%s'
                        from components.java
                    }
                }
            }
            """, coordinate.group(), coordinate.version(), dependenciesBlock, coordinate.artifact());
    }

    private void runPublish(MavenCoordinate coordinate) {
        String taskPath = ":" + subprojectName(coordinate) + ":publishMavenPublicationToMavenRepository";
        gradleInvoker.withArgs(taskPath, "--quiet").buildsSuccessfully();
    }

    private static String subprojectName(MavenCoordinate coordinate) {
        return coordinate.group() + "." + coordinate.artifact() + "_" + coordinate.version().replace('.', '_');
    }
}
