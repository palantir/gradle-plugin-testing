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

import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.execution.InvocationResult;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@GradlePluginTests
class MavenRepoTest {

    @BeforeEach
    void setupCommonDependencies(MavenRepo repo, RootProject root) {
        // Shared dependencies available to all tests
        repo.publish("com.palantir:service-a:1.0.0");
        root.buildGradle()
                .append(
                        """
                plugins {
                    id 'java-library'
                }
                """)
                .withMavenRepo(repo);
    }

    @Test
    void simple_dependency(RootProject root, MavenRepo repo, GradleInvoker gradle) {
        root.buildGradle()
                .append(
                        """
                dependencies {
                    implementation 'com.palantir:service-a:1.0.0'
                }
                """);

        InvocationResult result = gradle.withArgs("dependencies").buildsSuccessfully();

        assertThat(result.output()).contains("com.palantir:service-a:1.0.0");
    }

    @Test
    void multiple_modules_with_dependencies(RootProject root, MavenRepo repo, GradleInvoker gradle) {
        repo.publish(
                "com.external:library:1.0.0",
                "com.external:other:2.0.0 -> com.external:library:1.0.0|com.palantir:service-a:1.0.0");

        root.buildGradle()
                .append(
                        """
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
    void builder_pattern_with_custom_properties(RootProject root, MavenRepo repo, GradleInvoker gradle) {
        Module lib = Module.of("com.external:library:1.0.0")
                .targetCompatibility("1.8")
                .build();

        Module app = Module.of("com.external:app:2.0.0")
                .dependsOn("com.external:library:1.0.0")
                .dependsOn("com.palantir:service-a:1.0.0")
                .targetCompatibility("1.8")
                .build();

        repo.publish(lib, app);

        root.buildGradle()
                .append(
                        """
                dependencies {
                    implementation 'com.external:app:2.0.0'
                }
                """);

        InvocationResult result = gradle.withArgs("dependencies").buildsSuccessfully();

        assertThat(result.output()).contains("com.external:app:2.0.0");
        assertThat(result.output()).contains("com.external:library:1.0.0");
        assertThat(result.output()).contains("com.palantir:service-a:1.0.0");
    }

    @Test
    void can_compile_against_published_dependencies(RootProject root, MavenRepo repo, GradleInvoker gradle) {
        repo.publish("com.test:api:1.0.0");

        root.buildGradle()
                .append(
                        """
                dependencies {
                    implementation 'com.test:api:1.0.0'
                }
                """);

        root.mainSourceSet()
                .java()
                .writeClass(
                        """
                package example;

                public class Main {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                """);

        InvocationResult result = gradle.withArgs("compileJava").buildsSuccessfully();

        assertThat(result.output()).contains("BUILD SUCCESSFUL");
    }
}
