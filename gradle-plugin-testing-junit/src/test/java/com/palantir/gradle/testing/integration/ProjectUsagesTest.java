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

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import com.palantir.gradle.testing.project.SubProject;
import org.junit.jupiter.api.Test;

@GradlePluginTests
class ProjectUsagesTest {
    @Test
    void root_project_parameter(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            println "hello from ${path}"
            println "project name: ${name}"
            """);

        gradle.withArgs().buildsSuccessfully().assertThat().output().contains("hello from :");
        gradle.withArgs().buildsSuccessfully().assertThat().output().contains("project name: root");
    }

    @Test
    void root_project_parameter_with_custom_name_ending_in_project(GradleInvoker gradle, RootProject serviceProject) {
        serviceProject.buildGradle().append("""
            println "hello from ${path}"
            println "project name: ${name}"
            """);

        gradle.withArgs().buildsSuccessfully().assertThat().output().contains("hello from :");
        gradle.withArgs().buildsSuccessfully().assertThat().output().contains("project name: service");
    }

    @Test
    void root_project_parameter_with_custom_name_not_ending_in_project(GradleInvoker gradle, RootProject service) {
        service.buildGradle().append("""
            println "hello from ${path}"
            println "project name: ${name}"
            """);

        gradle.withArgs().buildsSuccessfully().assertThat().output().contains("hello from :");
        gradle.withArgs().buildsSuccessfully().assertThat().output().contains("project name: service");
    }

    @Test
    void sub_project_parameter_ending_in_project(GradleInvoker gradle, SubProject assetProject) {
        assetProject.buildGradle().append("""
            println "hello from ${path}"
            """);

        gradle.withArgs().buildsSuccessfully().assertThat().output().contains("hello from :asset");
    }

    void sub_project_parameter_ending_not_ending_in_project(GradleInvoker gradle, SubProject service) {
        service.buildGradle().append("""
            println "hello from ${path}"
            """);

        gradle.withArgs().buildsSuccessfully().assertThat().output().contains("hello from :service");
    }

    @Test
    void sub_project_manually(GradleInvoker gradle, RootProject rootProject) {
        SubProject subProject = rootProject.subproject("something");

        subProject.buildGradle().append("""
            println "hello from ${path}"
            """);

        gradle.withArgs().buildsSuccessfully().assertThat().output().contains("hello from :something");
    }

    @Test
    void two_layer_deep_sub_project(GradleInvoker gradle, SubProject serviceProject) {
        SubProject subSubProject = serviceProject.subproject("under-service");

        subSubProject.buildGradle().append("""
            println "hello from ${path}"
            """);

        gradle.withArgs().buildsSuccessfully().assertThat().output().contains("hello from :service:under-service");
    }

    @Test
    void can_request_the_same_subproject_multiple_times_without_issue(GradleInvoker gradle, RootProject rootProject) {
        SubProject subProject = rootProject.subproject("subproject");
        SubProject sameSubProject = rootProject.subproject("subproject");

        assertThat(sameSubProject.path()).isEqualTo(subProject.path());
        rootProject.settingsGradle().assertThat().content().containsOnlyOnce("subproject");

        subProject.buildGradle().append("""
            println "hello from ${path}"
            """);

        gradle.withArgs().buildsSuccessfully().assertThat().output().contains("hello from :subproject");
    }
}
