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

package com.palantir.gradle.testing.execution;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.RestrictedApi;
import com.palantir.gradle.testing.RestrictedCreation;
import com.palantir.gradle.testing.project.RootProject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class GradleWithJdksInvoker implements GradleInvoker {

    private final GradleInvoker gradleInvoker;
    private final RootProject rootProject;

    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public GradleWithJdksInvoker(Path rootProjectDir, GradleInvoker gradleInvoker) {
        this.rootProject = new RootProject(rootProjectDir);
        this.gradleInvoker = gradleInvoker;
    }

    @Override
    public GradleInvocation withArgs(String... args) {
        setupRootProject(rootProject);
        Path gradleJdksDirectory = rootProject.path().getParent().getParent().getParent();
        GradleInvocation wrapperInvocation = gradleInvoker.withArgs(
                "generateGradleJdkConfigs",
                "--onlyForCurrentOsArch",
                "--info",
                "-P__TESTING=true",
                String.format("-P__TESTING_GRADLE_USER_HOME=%s", gradleJdksDirectory));
        ProcessBuilder processBuilder = new ProcessBuilder()
                .command(rootProject
                        .path()
                        .resolve("gradle/gradle-jdks-setup.sh")
                        .toString());
        processBuilder.environment().put("GRADLE_USER_HOME", gradleJdksDirectory.toString());
        return new GradleWithJdksInvocation(
                wrapperInvocation, processBuilder, () -> getInvokerWithToolchainsConfigured(gradleJdksDirectory, args));
    }

    private GradleInvocation getInvokerWithToolchainsConfigured(Path gradleJdksDirectory, String... args) {
        String[] withJavaHome = ImmutableList.<String>builder()
                .add(args)
                .add(String.format(
                        "-Dorg.gradle.java.home=%s", getGradleJavaHome(rootProject.path(), gradleJdksDirectory)))
                .add("-P__TESTING=true")
                .add(String.format("-P__TESTING_GRADLE_USER_HOME=%s", gradleJdksDirectory))
                .build()
                .toArray(String[]::new);
        return gradleInvoker.withArgs(withJavaHome);
    }

    private static Path getGradleJavaHome(Path rootProjectDir, Path gradleJdksDirectory) {
        try {
            String majorVersion = Files.readString(rootProjectDir.resolve("gradle/gradle-daemon-jdk-version"))
                    .trim();
            try (Stream<Path> stream = Files.find(
                    rootProjectDir.resolve(String.format("gradle/jdks/%s", majorVersion)),
                    3,
                    (path, attr) -> path.getFileName().toString().equals("local-path") && attr.isRegularFile())) {
                String localPath = stream.findFirst()
                        .map(path -> {
                            try {
                                return Files.readString(path).trim();
                            } catch (IOException e) {
                                throw new UncheckedIOException("Failed to read the daemon jdk version path", e);
                            }
                        })
                        .orElseThrow();
                return gradleJdksDirectory.resolve("gradle-jdks").resolve(localPath);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to retrieve the java home directory", exception);
        }
    }

    private static void setupRootProject(RootProject rootProject) {
        rootProject.gradlePropertiesFile().appendProperty("palantir.jdk.setup.enabled", "true");
        rootProject.buildGradle().plugins().add("java");
        rootProject.buildGradle().plugins().add("com.palantir.jdks");
        rootProject.buildGradle().plugins().add("com.palantir.jdks.latest");
        rootProject.settingsGradle().plugins().add("com.palantir.jdks.settings");
    }
}
