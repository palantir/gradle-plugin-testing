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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.execution.InvocationResult;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.maven.MavenArtifact;
import com.palantir.gradle.testing.maven.MavenRepo;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@GradlePluginTests
class MavenRepoUsageTest {

    @BeforeEach
    void beforeEach(RootProject root, MavenRepo repo) {
        repo.publish(MavenArtifact.of("com.palantir:service-a:1.0.0"));
        root.buildGradle().plugins().add("java-library");
        root.buildGradle().withMavenRepo(repo);
    }

    @Test
    void simple_dependency(GradleInvoker gradle, RootProject root) {
        root.buildGradle().append("""
            dependencies {
                implementation 'com.palantir:service-a:1.0.0'
            }
            """);

        InvocationResult result = gradle.withArgs("dependencies").buildsSuccessfully();

        assertThat(result.output()).contains("com.palantir:service-a:1.0.0");
    }

    @Test
    void multiple_modules_with_dependencies(GradleInvoker gradle, RootProject root, MavenRepo repo) {
        repo.publish(
                MavenArtifact.of("com.external:library:1.0.0"),
                MavenArtifact.builder()
                        .coordinate("com.external:other:2.0.0")
                        .addDependency("com.external:library:1.0.0")
                        .addDependency("com.palantir:service-a:1.0.0")
                        .build());

        root.buildGradle().append("""
            dependencies {
                implementation 'com.external:other:2.0.0'
            }
            """);

        InvocationResult result = gradle.withArgs("dependencies").buildsSuccessfully();

        assertThat(result.output())
                .contains("com.external:other:2.0.0")
                .contains("com.external:library:1.0.0")
                .contains("com.palantir:service-a:1.0.0");
    }

    @Test
    void publishing_same_coordinate_twice_succeeds(GradleInvoker gradle, RootProject root, MavenRepo repo) {
        MavenArtifact lib = MavenArtifact.of("com.external:library:1.0.0");
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
    void same_dependency_declared_multiple_times(GradleInvoker gradle, RootProject root, MavenRepo repo) {
        repo.publish(MavenArtifact.builder()
                .coordinate("com.external:library:1.0.0")
                .addDependency("com.palantir:service-a:1.0.0")
                .addDependency("com.palantir:service-a:1.0.0")
                .build());

        root.buildGradle().append("""
            dependencies {
                implementation 'com.external:library:1.0.0'
            }
            """);

        InvocationResult result = gradle.withArgs("dependencies").buildsSuccessfully();

        assertThat(result.output()).contains("com.external:library:1.0.0").contains("com.palantir:service-a:1.0.0");
    }

    @Test
    void transitive_dependency_chain(GradleInvoker gradle, RootProject root, MavenRepo repo) {
        repo.publish(
                MavenArtifact.of("com.external:leaf:1.0.0"),
                MavenArtifact.builder()
                        .coordinate("com.external:middle:1.0.0")
                        .addDependency("com.external:leaf:1.0.0")
                        .build(),
                MavenArtifact.builder()
                        .coordinate("com.external:top:1.0.0")
                        .addDependency("com.external:middle:1.0.0")
                        .build());

        root.buildGradle().append("""
            dependencies {
                implementation 'com.external:top:1.0.0'
            }
            """);

        InvocationResult result = gradle.withArgs("dependencies").buildsSuccessfully();

        assertThat(result.output())
                .contains("com.external:top:1.0.0")
                .contains("com.external:middle:1.0.0")
                .contains("com.external:leaf:1.0.0");
    }

    @Test
    void dependency_validation_rejects_invalid_format() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> MavenArtifact.builder()
                        .coordinate("com.external:library:1.0.0")
                        .addDependency("com.external:lib")
                        .build())
                .withMessageContaining("Coordinate must be in format 'group:artifact:version'")
                .withMessageContaining("com.external:lib");
    }
}
