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
import com.google.common.base.Splitter;
import com.palantir.gradle.testing.execution.GradleVersion;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

/**
 * Internal class that manages the Gradle project structure for publishing modules.
 * This creates an independent Gradle project with subprojects for each module.
 */
final class PublisherProject {
    private static final String BUILD_GRADLE = "build.gradle";
    private static final String SETTINGS_GRADLE = "settings.gradle";

    private final Path projectRoot;
    private final Path mavenRepoPath;
    private boolean isGenerated = false;

    PublisherProject(Path projectRoot, Path mavenRepoPath) {
        this.projectRoot = Preconditions.checkNotNull(projectRoot, "projectRoot");
        this.mavenRepoPath = Preconditions.checkNotNull(mavenRepoPath, "mavenRepoPath");
    }

    void generateGradleFiles(List<Module> modules, GradleVersion gradleVersion) {
        if (isGenerated) {
            // Already generated, just add new subprojects
            addModulesToExistingProject(modules);
            return;
        }

        try {
            Files.createDirectories(projectRoot);

            // Generate root build.gradle
            String rootBuildGradle = generateRootBuildGradle();
            writeFile(projectRoot.resolve(BUILD_GRADLE), rootBuildGradle);

            // Generate settings.gradle
            List<String> subprojects = new ArrayList<>();
            for (Module module : modules) {
                String subprojectName = subprojectName(module);
                subprojects.add(subprojectName);
                generateSubproject(module, subprojectName);
            }

            String settingsGradle = generateSettingsGradle(subprojects);
            writeFile(projectRoot.resolve(SETTINGS_GRADLE), settingsGradle);

            isGenerated = true;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate publisher project", e);
        }
    }

    void runPublish(GradleVersion gradleVersion) {
        try {
            GradleConnector connector = GradleConnector.newConnector()
                    .forProjectDirectory(projectRoot.toFile())
                    .useGradleVersion(gradleVersion.version());

            try (ProjectConnection connection = connector.connect()) {
                connection
                        .newBuild()
                        .forTasks("publishMavenPublicationToMavenRepository")
                        .setStandardOutput(System.out)
                        .setStandardError(System.err)
                        .run();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish modules", e);
        }
    }

    void runPublish(GradleVersion gradleVersion, Module module) {
        try {
            GradleConnector connector = GradleConnector.newConnector()
                    .forProjectDirectory(projectRoot.toFile())
                    .useGradleVersion(gradleVersion.version());

            String taskPath = ":" + subprojectName(module) + ":publishMavenPublicationToMavenRepository";

            try (ProjectConnection connection = connector.connect()) {
                connection
                        .newBuild()
                        .forTasks(taskPath)
                        .setStandardOutput(System.out)
                        .setStandardError(System.err)
                        .run();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish module: " + module.coordinate(), e);
        }
    }

    private void addModulesToExistingProject(List<Module> modules) {
        try {
            // Read existing settings.gradle
            Path settingsPath = projectRoot.resolve(SETTINGS_GRADLE);
            String existingSettings = Files.readString(settingsPath);

            // Parse existing includes
            List<String> allSubprojects = new ArrayList<>();
            for (String line : Splitter.on('\n').split(existingSettings)) {
                if (line.trim().startsWith("include")) {
                    // Extract subproject names from include statements
                    String includeContent = line.substring(line.indexOf("include") + 7).trim();
                    for (String project : Splitter.on(',').split(includeContent)) {
                        String cleaned = project.trim().replace("'", "").replace("\"", "");
                        if (!cleaned.isEmpty()) {
                            allSubprojects.add(cleaned);
                        }
                    }
                }
            }

            // Add new modules
            for (Module module : modules) {
                String subprojectName = subprojectName(module);
                if (!allSubprojects.contains(subprojectName)) {
                    allSubprojects.add(subprojectName);
                    generateSubproject(module, subprojectName);
                }
            }

            // Regenerate settings.gradle
            String newSettings = generateSettingsGradle(allSubprojects);
            writeFile(settingsPath, newSettings);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to add modules to existing project", e);
        }
    }

    private void generateSubproject(Module module, String subprojectName) throws IOException {
        Path subprojectDir = projectRoot.resolve(subprojectName);
        Files.createDirectories(subprojectDir);

        String subprojectBuildGradle = generateSubprojectBuildGradle(module);
        writeFile(subprojectDir.resolve(BUILD_GRADLE), subprojectBuildGradle);

        // Create a dummy source file so Gradle can build the jar
        Path javaDir = subprojectDir.resolve("src/main/java");
        Files.createDirectories(javaDir);
        Path dummyFile = javaDir.resolve("Dummy.java");
        writeFile(
                dummyFile,
                """
                public class Dummy {
                    // Empty class for test dependencies
                }
                """);
    }

    private String generateRootBuildGradle() {
        String mavenRepoUrl = mavenRepoPath.toUri().toString();
        return """
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
            """.formatted(mavenRepoUrl, mavenRepoUrl);
    }

    private String generateSubprojectBuildGradle(Module module) {
        StringBuilder script = new StringBuilder();

        script.append("group = '").append(module.group()).append("'\n");
        script.append("version = '").append(module.version()).append("'\n\n");

        String javaVersion = "VERSION_" + module.targetCompatibility().replace(".", "_");
        script.append("java {\n");
        script.append("    sourceCompatibility = JavaVersion.").append(javaVersion).append("\n");
        script.append("    targetCompatibility = JavaVersion.").append(javaVersion).append("\n");
        script.append("}\n\n");

        if (!module.dependencies().isEmpty()) {
            script.append("dependencies {\n");
            for (String dependency : module.dependencies()) {
                script.append("    api '").append(dependency).append("'\n");
            }
            script.append("}\n\n");
        }

        script.append("publishing {\n");
        script.append("    publications {\n");
        script.append("        maven(MavenPublication) {\n");
        script.append("            artifactId = '")
                .append(module.artifact())
                .append("'\n");
        script.append("            from components.java\n");
        script.append("        }\n");
        script.append("    }\n");
        script.append("}\n");

        return script.toString();
    }

    private String generateSettingsGradle(List<String> subprojects) {
        StringBuilder script = new StringBuilder();
        script.append("rootProject.name = 'maven-repo-publisher'\n\n");

        if (!subprojects.isEmpty()) {
            script.append("include ");
            script.append(
                    String.join(", ", subprojects.stream().map(s -> "'" + s + "'").toList()));
            script.append("\n");
        }

        return script.toString();
    }

    private String subprojectName(Module module) {
        // Create a unique subproject name from the module coordinate
        return module.group() + "." + module.artifact() + "_" + module.version().replace('.', '_');
    }

    private void writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }
}
