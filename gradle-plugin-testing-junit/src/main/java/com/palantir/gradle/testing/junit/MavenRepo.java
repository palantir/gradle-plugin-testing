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
import com.google.errorprone.annotations.RestrictedApi;
import com.palantir.gradle.testing.RestrictedCreation;
import com.palantir.gradle.testing.execution.GradleVersion;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A test Maven repository that can publish modules for use in integration tests.
 * <p>
 * This class creates an independent Gradle project structure and publishes modules using Gradle's
 * {@code publishMavenPublicationToMavenRepository} task.
 * <p>
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
    private final Path repoPath;
    private final GradleVersion gradleVersion;
    private final PublisherProject publisherProject;
    private final List<Module> publishedModules = new ArrayList<>();

    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public MavenRepo(Path repoPath, GradleVersion gradleVersion) {
        this.repoPath = Preconditions.checkNotNull(repoPath, "repoPath");
        this.gradleVersion = Preconditions.checkNotNull(gradleVersion, "gradleVersion");
        this.publisherProject = new PublisherProject(
                repoPath.getParent().resolve(".maven-repo-publisher"), repoPath, gradleVersion);
    }

    /**
     * Publishes one or more modules to the Maven repository.
     * <p>
     * Modules can be specified in two formats:
     * <ul>
     *   <li>Simple: "group:artifact:version"</li>
     *   <li>With dependencies: "group:artifact:version -> dep1:dep2:dep3|dep4:dep5:dep6"</li>
     * </ul>
     *
     * @param modules one or more module specifications
     */
    public void publish(String... modules) {
        Preconditions.checkArgument(modules.length > 0, "At least one module must be provided");
        List<Module> parsedModules = Arrays.stream(modules)
                .map(this::parseModule)
                .toList();
        publish(parsedModules.toArray(new Module[0]));
    }

    /**
     * Publishes one or more modules to the Maven repository using the builder pattern.
     * Modules are published in the order provided to ensure dependencies are available.
     *
     * @param modules one or more Module instances
     */
    public void publish(Module... modules) {
        Preconditions.checkArgument(modules.length > 0, "At least one module must be provided");

        // Publish each module individually to ensure dependencies are available
        for (Module module : modules) {
            publishedModules.add(module);
            publisherProject.generateGradleFiles(List.of(module));
            publisherProject.runPublish(module);
        }
    }

    /**
     * Returns the path to the Maven repository directory.
     */
    public Path path() {
        return repoPath;
    }

    /**
     * Returns the Maven repository URL for use in repository blocks.
     */
    public String url() {
        return repoPath.toUri().toString();
    }

    /**
     * Returns a Gradle repository block configuration string that can be added to build.gradle.
     */
    public String repositoryBlock() {
        return "maven { url = uri('%s') }".formatted(url());
    }

    private Module parseModule(String moduleString) {
        if (moduleString.contains("->")) {
            return Module.parseWithDependencies(moduleString);
        } else {
            return Module.parseSimple(moduleString);
        }
    }

    @Override
    public String toString() {
        return "MavenRepo[path=%s, modules=%d]".formatted(repoPath, publishedModules.size());
    }
}
