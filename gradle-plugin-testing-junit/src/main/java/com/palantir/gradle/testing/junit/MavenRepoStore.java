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

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.execution.GradleVersion;
import com.palantir.gradle.testing.maven.MavenRepo;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

/**
 * Store for managing the singleton MavenRepo instance per test class.
 */
final class MavenRepoStore {
    private static final Namespace NAMESPACE = Namespace.create(MavenRepoStore.class);
    private static final String MAVEN_REPO_KEY = "mavenRepo";

    private MavenRepoStore() {}

    static MavenRepo mavenRepo(ExtensionContext context) {
        return context.getStore(NAMESPACE)
                .getOrComputeIfAbsent(MAVEN_REPO_KEY, _ignored -> initializeMavenRepo(context), MavenRepo.class);
    }

    private static MavenRepo initializeMavenRepo(ExtensionContext context) {
        GradleVersion gradleVersion = GradleVersionStore.gradleVersion(context);
        Path repositoryDirectory = RootProjectStore.rootProject(context).path().resolve("build/mavenrepo");

        clearDirectory(repositoryDirectory);

        return new MavenRepo(repositoryDirectory, gradleVersion, GradleInvoker.readGradleDistributionBaseUrl(context));
    }

    private static void clearDirectory(Path mavenRepoDirectory) {
        try {
            FileUtils.deleteDirectory(mavenRepoDirectory.toFile());
            Files.createDirectories(mavenRepoDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to recreate the maven repository directory", e);
        }
    }
}
