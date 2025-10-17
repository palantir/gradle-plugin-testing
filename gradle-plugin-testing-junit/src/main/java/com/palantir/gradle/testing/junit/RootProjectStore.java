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

import com.google.common.collect.Lists;
import com.palantir.gradle.testing.execution.GradleVersion;
import com.palantir.gradle.testing.project.RootProject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

final class RootProjectStore {
    private static final Namespace NAMESPACE = Namespace.create(GradleProjectParameterResolver.class);
    private static final Path GRADLE_TESTING_DIR =
            Path.of("build/gradle-plugin-testing/").toAbsolutePath();
    private static final String PROJECT_DIR_KEY = "projectDir";
    private static final Pattern INVALID_FILENAME_CHARS = Pattern.compile("[\\\\/:*?\"<>|\\x00-\\x1F]");

    public static RootProject rootProject(ExtensionContext extensionContext) {
        return new RootProject(rootProjectDir(extensionContext));
    }

    public static Path rootProjectDir(ExtensionContext context) {
        return context.getStore(NAMESPACE)
                .getOrComputeIfAbsent(PROJECT_DIR_KEY, _ignored -> initializeRootProjectDir(context), Path.class);
    }

    private static Path initializeRootProjectDir(ExtensionContext context) {
        GradleVersion gradleVersion = GradleVersionStore.gradleVersion(context);

        String projectDirFragmentWithoutGradleVersion = contextsFromBelowRootDownTo(context)
                .map(ExtensionContext::getDisplayName)
                .filter(Predicate.not(Predicate.isEqual("Gradle " + gradleVersion)))
                .map(RootProjectStore::replaceCharsInvalidInFilenames)
                .collect(Collectors.joining("/"));

        Path projectDir = GRADLE_TESTING_DIR
                .resolve(projectDirFragmentWithoutGradleVersion)
                .resolve(gradleVersion.version());

        clearDirectory(projectDir);

        RootProject rootProject = new RootProject(projectDir);
        rootProject.settingsGradle().createEmpty();

        return projectDir;
    }

    private static Stream<ExtensionContext> contextsFromBelowRootDownTo(ExtensionContext extensionContext) {
        return Lists.reverse(contextsUpToAndIncludingRoot(extensionContext)
                        .takeWhile(context -> context.getParent().isPresent())
                        .toList())
                .stream();
    }

    private static String replaceCharsInvalidInFilenames(String displayName) {
        return INVALID_FILENAME_CHARS.matcher(displayName).replaceAll("_");
    }

    private static Stream<ExtensionContext> contextsUpToAndIncludingRoot(ExtensionContext extensionContext) {
        return Stream.iterate(
                        Optional.of(extensionContext),
                        Optional::isPresent,
                        optionalExtensionContext ->
                                optionalExtensionContext.get().getParent())
                .map(Optional::get);
    }

    private static void clearDirectory(Path projectDir) {
        try {
            FileUtils.deleteDirectory(projectDir.toFile());
            Files.createDirectories(projectDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to recreate the test directory", e);
        }
    }

    private RootProjectStore() {}
}
