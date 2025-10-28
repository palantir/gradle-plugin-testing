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

import com.palantir.gradle.testing.execution.GradleVersion;
import java.nio.file.Path;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

/**
 * Store for managing the singleton MavenRepo instance per test class.
 */
final class MavenRepoStore {
    private static final Namespace NAMESPACE = Namespace.create(MavenRepoStore.class);
    private static final String MAVEN_REPO_KEY = "mavenRepo";

    private MavenRepoStore() {}

    static MavenRepo mavenRepo(ExtensionContext extensionContext) {
        return extensionContext
                .getStore(NAMESPACE)
                .getOrComputeIfAbsent(
                        MAVEN_REPO_KEY,
                        _key -> {
                            // Get the root project path to determine where to create the maven repo
                            Path rootProjectPath = RootProjectStore.rootProject(extensionContext)
                                    .path();
                            Path mavenRepoPath = rootProjectPath.getParent().resolve("mavenrepo");

                            GradleVersion gradleVersion = GradleVersionStore.gradleVersion(extensionContext);

                            return new MavenRepo(mavenRepoPath, gradleVersion);
                        },
                        MavenRepo.class);
    }
}
