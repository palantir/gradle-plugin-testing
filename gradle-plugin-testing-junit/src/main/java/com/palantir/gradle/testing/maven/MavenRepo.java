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
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * A test Maven repository that can publish modules for use in integration tests.
 * Usage:
 * <pre>
 * &#64;BeforeEach
 * void setupCommonDependencies(MavenRepo repo, RootProject root) {
 *     repo.publish("com.palantir:service-a:1.0.0");
 *     root.buildGradle().withMavenRepo(repo);
 * }
 *
 * &#64;Test
 * void test(MavenRepo repo) {
 *     repo.publish(
 *         "com.external:library:1.0.0",
 *         "com.external:other:2.0.0 -> com.external:library:1.0.0|com.palantir:service-a:1.0.0"
 *     );
 * }
 * </pre>
 */
public final class MavenRepo {
    private final URI repoUri;
    private final MavenRepoPublisher publisher;

    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public MavenRepo(Path repoDir, GradleVersion gradleVersion) {
        this.repoUri = repoDir.toUri();
        this.publisher = new MavenRepoPublisher(repoDir.resolve("testgenrepo"), repoUri, gradleVersion);
    }

    /**
     * Publishes one or more modules to the Maven repository using the builder pattern.
     * Modules are published in the order provided to ensure dependencies are available.
     *
     * @param modules one or more MavenCoordinate instances
     */
    public void publish(MavenCoordinate... modules) {
        publish(List.of(modules));
    }

    /**
     * Publishes one or more modules to the Maven repository using the builder pattern.
     * Modules are published in the order provided to ensure dependencies are available.
     *
     * @param modules a list of MavenCoordinate instances
     */
    public void publish(List<MavenCoordinate> modules) {
        publisher.publish(modules);
    }

    /**
     * Returns a Gradle repository block configuration string that can be added to build.gradle in the form:
     * <pre>{@code
     * maven { url = uri('repoUri') }
     * }</pre>
     */
    public String repositoryBlock() {
        return "maven { url = uri('%s') }".formatted(repoUri);
    }
}
