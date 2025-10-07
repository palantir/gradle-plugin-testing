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

package com.palantir.example;

import com.palantir.gradle.testing.GradlePluginTests;
import com.palantir.gradle.testing.execution.Gradlew;
import com.palantir.gradle.testing.project.RootProject;
import com.palantir.gradle.testing.project.SubProject;
import org.junit.jupiter.api.Test;

@GradlePluginTests
public final class GradleVersionOutputterTest {
    @Test
    void noop_test(Gradlew gradlew, RootProject rootProject) {
        rootProject
                .buildFile()
                .append(
                        """
            import org.gradle.util.GradleVersion
            file('gradle-version').text = GradleVersion.current()
            """);

        SubProject subproject = rootProject.addSubproject("subproject");

        subproject.buildFile().appendLine("apply plugin: 'java-library'");

        subproject
                .mainSourceSet()
                .java()
                .writeClass(
                        """
            package app;
            public static class Main {
                public static void main(String[] args) {
                    System.out.println("hello");
                }
            }
            """);

        SubProject subsubproject = subproject.addSubproject("subsubproject");

        subsubproject
                .mainSourceSet()
                .srcDir("conjure")
                .yamlFile("conjure.yml")
                .append("""
            looky: here
            yaml:
              with: highlighting
            """);

        gradlew.withArgs("help", "--no-build-cache").buildSuccessfully();

        rootProject.file("gradle-version").assertThat().hasContent("hello");
    }
}
