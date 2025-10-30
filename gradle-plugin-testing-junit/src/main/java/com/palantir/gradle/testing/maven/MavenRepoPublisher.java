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

    void publish(List<MavenCoordinate> modules) {
        modules.forEach(module -> {
            createSubproject(module, subprojectName(module));
            runPublish(module);
        });
    }

    private void createSubproject(MavenCoordinate module, String subprojectName) {
        SubProject subproject = rootProject.subproject(subprojectName);

        String dependenciesBlock = module.dependencies().isEmpty()
                ? ""
                : """
                dependencies {
                    %s
                }
                """.formatted(module.dependencies().stream()
                        .map("    api '%s'"::formatted)
                        .collect(Collectors.joining("\n")));

        subproject
                .buildGradle()
                .overwrite(
                        """
                        group = '%s'
                        version = '%s'

                        java {
                            sourceCompatibility = JavaVersion.%s
                            targetCompatibility = JavaVersion.%s
                        }

                        %s
                        publishing {
                            publications {
                                maven(MavenPublication) {
                                    artifactId = '%s'
                                    from components.java
                                }
                            }
                        }
                        """,
                        module.group(),
                        module.version(),
                        module.targetCompatibility().name(),
                        module.targetCompatibility().name(),
                        dependenciesBlock,
                        module.artifact());
    }

    private void runPublish(MavenCoordinate module) {
        String taskPath = ":" + subprojectName(module) + ":publishMavenPublicationToMavenRepository";
        gradleInvoker.withArgs(taskPath, "--quiet").buildsSuccessfully();
    }

    private static String subprojectName(MavenCoordinate module) {
        return module.group() + "." + module.artifact() + "_" + module.version().replace('.', '_');
    }
}
