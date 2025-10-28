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

package com.palantir.gradle.testing.junit;

import com.google.common.base.Preconditions;
import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.execution.GradleVersion;
import com.palantir.gradle.testing.project.RootProject;
import com.palantir.gradle.testing.project.SubProject;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Internal class that manages the Gradle project structure for publishing modules.
 * This creates an independent Gradle project with subprojects for each module.
 */
final class PublisherProject {
    private final RootProject rootProject;
    private final GradleInvoker gradleInvoker;
    private final Path mavenRepoPath;
    private final Set<String> publishedSubprojects = new HashSet<>();
    private boolean isInitialized = false;

    PublisherProject(Path projectRoot, Path mavenRepoPath, GradleVersion gradleVersion) {
        Preconditions.checkNotNull(projectRoot, "projectRoot");
        this.mavenRepoPath = Preconditions.checkNotNull(mavenRepoPath, "mavenRepoPath");
        this.rootProject = new RootProject(projectRoot);
        this.gradleInvoker = new GradleInvoker(projectRoot, gradleVersion);
    }

    void generateGradleFiles(List<Module> modules) {
        if (!isInitialized) {
            initializeProject();
        }

        for (Module module : modules) {
            String subprojectName = subprojectName(module);
            if (!publishedSubprojects.contains(subprojectName)) {
                createSubproject(module, subprojectName);
                publishedSubprojects.add(subprojectName);
            }
        }
    }

    private void initializeProject() {
        rootProject.settingsGradle().rootProjectName("maven-repo-publisher");

        String mavenRepoUrl = mavenRepoPath.toUri().toString();
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

        isInitialized = true;
    }

    private void createSubproject(Module module, String subprojectName) {
        SubProject subproject = rootProject.subproject(subprojectName);

        String javaVersion = "VERSION_" + module.targetCompatibility().replace(".", "_");
        StringBuilder buildScript = new StringBuilder();

        buildScript.append("group = '").append(module.group()).append("'\n");
        buildScript.append("version = '").append(module.version()).append("'\n\n");

        buildScript.append("java {\n");
        buildScript
                .append("    sourceCompatibility = JavaVersion.")
                .append(javaVersion)
                .append("\n");
        buildScript
                .append("    targetCompatibility = JavaVersion.")
                .append(javaVersion)
                .append("\n");
        buildScript.append("}\n\n");

        if (!module.dependencies().isEmpty()) {
            buildScript.append("dependencies {\n");
            for (String dependency : module.dependencies()) {
                buildScript.append("    api '").append(dependency).append("'\n");
            }
            buildScript.append("}\n\n");
        }

        buildScript.append("publishing {\n");
        buildScript.append("    publications {\n");
        buildScript.append("        maven(MavenPublication) {\n");
        buildScript
                .append("            artifactId = '")
                .append(module.artifact())
                .append("'\n");
        buildScript.append("            from components.java\n");
        buildScript.append("        }\n");
        buildScript.append("    }\n");
        buildScript.append("}\n");

        subproject.buildGradle().overwrite(buildScript.toString());

        // Create dummy source file
        subproject.mainSourceSet().java().writeClass("""
            public class Dummy {
                // Empty class for test dependencies
            }
            """);
    }

    void runPublish(Module module) {
        String taskPath = ":" + subprojectName(module) + ":publishMavenPublicationToMavenRepository";
        gradleInvoker.withArgs(taskPath).buildsSuccessfully();
    }

    private static String subprojectName(Module module) {
        // Create a unique subproject name from the module coordinate
        return module.group() + "." + module.artifact() + "_" + module.version().replace('.', '_');
    }
}
