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

package com.palantir.gradle.testing.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.execution.InvocationResult;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.maven.MavenCoordinate;
import com.palantir.gradle.testing.maven.MavenRepo;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@GradlePluginTests
class MavenRepoUsageTest {

    @BeforeEach
    void beforeEach(MavenRepo repo, RootProject root) {
        repo.publish(MavenCoordinate.from("com.palantir:service-a:1.0.0").build());
        root.buildGradle().append("""
            plugins {
                id 'java-library'
            }
            """).withMavenRepo(repo);
    }

    @Test
    void simple_dependency(RootProject root, GradleInvoker gradle) {
        root.buildGradle().append("""
            dependencies {
                implementation 'com.palantir:service-a:1.0.0'
            }
            """);

        InvocationResult result = gradle.withArgs("dependencies").buildsSuccessfully();

        assertThat(result.output()).contains("com.palantir:service-a:1.0.0");
    }

    @Test
    void simple_dependency_in_buildscript(RootProject root, MavenRepo repo, GradleInvoker gradle) {
        root.buildGradle().prepend("""
            buildscript {
                // .withMavenRepo(repo) only adds to the main repo block so need to manually add
                repositories {
                    %s
                }
                dependencies {
                    classpath 'com.palantir:service-a:1.0.0'
                }
            }
            """, repo.repositoryBlock());

        InvocationResult result = gradle.withArgs("buildEnvironment").buildsSuccessfully();

        assertThat(result.output()).contains("com.palantir:service-a:1.0.0");
    }

    @Test
    void multiple_modules_with_dependencies(RootProject root, MavenRepo repo, GradleInvoker gradle) {
        repo.publish(
                MavenCoordinate.from("com.external:library:1.0.0").build(),
                MavenCoordinate.from("com.external:other:2.0.0")
                        .addDependencies("com.external:library:1.0.0")
                        .addDependencies("com.palantir:service-a:1.0.0")
                        .build());

        root.buildGradle().append("""
            dependencies {
                implementation 'com.external:other:2.0.0'
            }
            """);

        InvocationResult result = gradle.withArgs("dependencies").buildsSuccessfully();

        assertThat(result.output()).contains("com.external:other:2.0.0");
        assertThat(result.output()).contains("com.external:library:1.0.0");
        assertThat(result.output()).contains("com.palantir:service-a:1.0.0");
    }

    @Test
    void publishing_same_coordinate_twice_succeeds(RootProject root, MavenRepo repo, GradleInvoker gradle) {
        MavenCoordinate lib = MavenCoordinate.from("com.external:library:1.0.0").build();
        repo.publish(lib);
        repo.publish(lib);

        root.buildGradle().append("""
            dependencies {
                implementation 'com.external:library:1.0.0'
            }
            """);

        InvocationResult result = gradle.withArgs("dependencies").buildsSuccessfully();

        assertThat(result.output()).contains("com.external:library:1.0.0");
    }

    @Test
    void same_dependency_declared_multiple_times(RootProject root, MavenRepo repo, GradleInvoker gradle) {
        repo.publish(MavenCoordinate.from("com.external:library:1.0.0")
                .addDependencies("com.palantir:service-a:1.0.0")
                .addDependencies("com.palantir:service-a:1.0.0")
                .build());

        root.buildGradle().append("""
            dependencies {
                implementation 'com.external:library:1.0.0'
            }
            """);

        InvocationResult result = gradle.withArgs("dependencies").buildsSuccessfully();

        assertThat(result.output()).contains("com.external:library:1.0.0");
        assertThat(result.output()).contains("com.palantir:service-a:1.0.0");
    }

    @Test
    void transitive_dependency_chain(RootProject root, MavenRepo repo, GradleInvoker gradle) {
        repo.publish(
                MavenCoordinate.from("com.external:leaf:1.0.0").build(),
                MavenCoordinate.from("com.external:middle:1.0.0")
                        .addDependencies("com.external:leaf:1.0.0")
                        .build(),
                MavenCoordinate.from("com.external:top:1.0.0")
                        .addDependencies("com.external:middle:1.0.0")
                        .build());

        root.buildGradle().append("""
            dependencies {
                implementation 'com.external:top:1.0.0'
            }
            """);

        InvocationResult result = gradle.withArgs("dependencies").buildsSuccessfully();

        assertThat(result.output()).contains("com.external:top:1.0.0");
        assertThat(result.output()).contains("com.external:middle:1.0.0");
        assertThat(result.output()).contains("com.external:leaf:1.0.0");
    }

    @Test
    void dependency_validation_rejects_invalid_format() {
        assertThatThrownBy(() -> MavenCoordinate.from("com.external:library:1.0.0")
                        .addDependencies("com.external:lib")
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Coordinate must be in format 'group:artifact:version'")
                .hasMessageContaining("com.external:lib");
    }
}
