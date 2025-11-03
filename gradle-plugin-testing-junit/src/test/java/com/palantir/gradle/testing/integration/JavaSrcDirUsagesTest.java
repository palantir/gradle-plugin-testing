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

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.Test;

@GradlePluginTests
class JavaSrcDirUsagesTest {
    @Test
    void can_use_java_src_dir(GradleInvoker gradle, RootProject rootProject) {
        rootProject.mainSourceSet().java().writeClass("""
            package example;
            public class Main {
                public static void main(String[] args) {
                    System.out.println("Hello world!");
                }
            }
            """);

        rootProject.buildGradle().append("""
            apply plugin: 'application'

            application {
                mainClass = 'example.Main'
            }
            """);

        gradle.withArgs("run").buildsSuccessfully().assertThat().output().contains("Hello world!");

        rootProject
                .mainSourceSet()
                .java()
                .fileByClassName("example.Main")
                .edit(text -> text.replace("world", "universe"));

        gradle.withArgs("run").buildsSuccessfully().assertThat().output().contains("Hello universe!");
    }
}
