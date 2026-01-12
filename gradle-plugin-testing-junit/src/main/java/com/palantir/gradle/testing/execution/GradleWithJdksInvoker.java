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
import com.google.common.collect.Streams;
import com.google.errorprone.annotations.RestrictedApi;
import com.palantir.gradle.testing.RestrictedCreation;
import com.palantir.gradle.testing.project.RootProject;
import com.palantir.platform.Architecture;
import com.palantir.platform.OperatingSystem;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public final class GradleWithJdksInvoker implements GradleInvoker {

    private static final Architecture arch = Architecture.get();
    private static final OperatingSystem os = OperatingSystem.get();
    private final GradleInvoker gradleInvoker;
    private final RootProject rootProject;
    // RunJavaToolchains is running "./gradlew" and the "gradle-jdks-settings" plugin cannot be found
    private final Set<String> ignoredTasks = Set.of("runJavaToolchains");
    private final List<String> ignoredTasksWithFlag =
            ignoredTasks.stream().flatMap(task -> Stream.of("-x", task)).collect(ImmutableList.toImmutableList());

    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public GradleWithJdksInvoker(Path rootProjectDir, GradleInvoker gradleInvoker) {
        this.rootProject = new RootProject(rootProjectDir);
        this.gradleInvoker = gradleInvoker;
    }

    @Override
    public GradleInvocation withArgs(String... args) {
        setupRootProject(rootProject);
        GradleInvocation generateGradleJdkConfigs = gradleInvoker.withArgs(ImmutableList.<String>builder()
                .add("generateGradleJdkConfigs", "setupJdks")
                .addAll(ignoredTasksWithFlag)
                .build()
                .toArray(String[]::new));

        return new GradleWithJdksInvocation(generateGradleJdkConfigs, () -> getInvokerWithToolchainsConfigured(args));
    }

    @Override
    public GradleVersion getGradleVersion() {
        return gradleInvoker.getGradleVersion();
    }

    // this needs to be called after the daemon jdk version is rendered (by generateGradleJdkConfigs)
    private GradleInvocation getInvokerWithToolchainsConfigured(String... args) {
        List<String> argsWithExcludedTasks = Streams.concat(
                        Stream.of(args).filter(arg -> !ignoredTasks.contains(arg)), ignoredTasksWithFlag.stream())
                .toList();
        String[] withJavaHome = ImmutableList.<String>builder()
                .addAll(argsWithExcludedTasks)
                .add(String.format("-Dorg.gradle.java.home=%s", getGradleJavaHome(rootProject.path())))
                .build()
                .toArray(String[]::new);
        return gradleInvoker.withArgs(withJavaHome);
    }

    @SuppressWarnings("checkstyle:NestedTryDepth")
    private static Path getGradleJavaHome(Path rootProjectDir) {
        try {
            String majorVersion = Files.readString(rootProjectDir.resolve("gradle/gradle-daemon-jdk-version"))
                    .trim();

            try (Stream<Path> stream = Files.find(
                    rootProjectDir.resolve(
                            String.format("gradle/jdks/%s/%s/%s", majorVersion, os.uiName(), arch.uiName())),
                    1,
                    (path, attr) -> path.getFileName().toString().equals("local-path") && attr.isRegularFile())) {
                String localPath = stream.findFirst()
                        .map(GradleWithJdksInvoker::getLocalPath)
                        .orElseThrow(() -> new RuntimeException(
                                String.format("Failed to find the JDK local path for majorVersion %s", majorVersion)));
                return getGradleJdksDirectory(localPath);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to retrieve the gradle daemon jdk path", e);
        }
    }

    private static @NotNull String getLocalPath(Path path) {
        try {
            return Files.readString(path).trim();
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Failed to read the path %s", path), e);
        }
    }

    public static Path getGradleJdksDirectory(String localJdkPath) {
        return Path.of(Optional.ofNullable(System.getenv("GRADLE_USER_HOME"))
                        .orElseGet(() -> System.getProperty("user.home") + "/.gradle"))
                .resolve("gradle-jdks")
                .resolve(localJdkPath);
    }

    private static void setupRootProject(RootProject rootProject) {
        rootProject.gradlePropertiesFile().appendProperty("palantir.jdk.setup.enabled", "true");
        rootProject.buildGradle().plugins().add("java");
        rootProject.buildGradle().plugins().add("com.palantir.jdks");
        rootProject.settingsGradle().plugins().add("com.palantir.jdks.settings");
    }
}
