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

import com.google.errorprone.annotations.RestrictedApi;
import com.palantir.gradle.testing.RestrictedCreation;
import com.palantir.gradle.testing.execution.GradleVersion;
import java.nio.file.Path;
import java.util.List;

/**
 * A Maven repository that tests can resolve artifacts against. This allows you to publish fake modules with
 * correct Maven metadata (POM files with dependencies) for use in integration tests.
 *
 * <p>To have your build under test use this repository, call {@code root.buildGradle().withMavenRepo(repo)}.
 *
 * <p>Usage:
 * <pre>
 * &#64;BeforeEach
 * void setupCommonDependencies(MavenRepo repo, RootProject root) {
 *     repo.publish(MavenArtifact.of("com.palantir:service-a:1.0.0"));
 *     root.buildGradle().withMavenRepo(repo);
 * }
 *
 * &#64;Test
 * void test(MavenRepo repo) {
 *     repo.publish(
 *         MavenArtifact.of("com.external:library:1.0.0"),
 *         MavenArtifact.builder()
 *             .coordinate("com.external:other:2.0.0")
 *             .addDependency("com.external:library:1.0.0")
 *             .addDependency("com.palantir:service-a:1.0.0")
 *             .build()
 *     );
 * }
 * </pre>
 */
public final class MavenRepo {
    private final Path path;
    private final MavenRepoPublisher publisher;

    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public MavenRepo(Path repoDir, GradleVersion gradleVersion) {
        this.path = repoDir.resolve("repositoryRoot").toAbsolutePath();
        this.publisher = new MavenRepoPublisher(repoDir.resolve("repositoryPublisherProject"), path, gradleVersion);
    }

    /**
     * Publishes one or more artifacts to the Maven repository.
     *
     * @param artifacts one or more {@link MavenArtifact} instances
     */
    public void publish(MavenArtifact... artifacts) {
        publish(List.of(artifacts));
    }

    /**
     * Publishes a list of artifacts to the Maven repository.
     *
     * @param artifacts a list of MavenArtifact instances
     */
    public void publish(List<MavenArtifact> artifacts) {
        publisher.publish(artifacts);
    }

    public Path path() {
        return path;
    }
}
