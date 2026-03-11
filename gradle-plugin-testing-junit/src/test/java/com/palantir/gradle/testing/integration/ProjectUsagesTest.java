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
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.IncludedBuild;
import com.palantir.gradle.testing.project.RootProject;
import com.palantir.gradle.testing.project.SubProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
    void root_project_parameter_with_different_name_still_gets_named_root(
            GradleInvoker gradle, RootProject serviceProject) {
        serviceProject.buildGradle().append("""
            println "hello from ${path}"
            println "project name: ${name}"
            """);

        gradle.withArgs().buildsSuccessfully().assertThat().output().contains("hello from :");
        gradle.withArgs().buildsSuccessfully().assertThat().output().contains("project name: root");
    }

    @Test
    void root_project_can_be_renamed_explicitly(GradleInvoker gradle, RootProject rootProject) {
        rootProject.settingsGradle().rootProjectName("custom-service");
        rootProject.buildGradle().append("""
            println "hello from ${path}"
            println "project name: ${name}"
            """);

        gradle.withArgs().buildsSuccessfully().assertThat().output().contains("hello from :");
        gradle.withArgs().buildsSuccessfully().assertThat().output().contains("project name: custom-service");
    }

    @Test
    void sub_project_uses_exact_parameter_name(GradleInvoker gradle, SubProject assetProject) {
        assetProject.buildGradle().append("""
            println "hello from ${path}"
            """);

        gradle.withArgs().buildsSuccessfully().assertThat().output().contains("hello from :assetProject");
    }

    @Test
    void sub_project_parameter_not_ending_in_project(GradleInvoker gradle, SubProject service) {
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

        gradle.withArgs()
                .buildsSuccessfully()
                .assertThat()
                .output()
                .contains("hello from :serviceProject:under-service");
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

    @Test
    void included_build_uses_exact_parameter_name(GradleInvoker gradle, IncludedBuild myLib) {
        myLib.buildGradle().append("""
            println "included build name: ${name}"
            """);

        gradle.withArgs().buildsSuccessfully().assertThat().output().contains("included build name: myLib");
    }

    @Test
    void included_build_manually(GradleInvoker gradle, RootProject rootProject) {
        IncludedBuild includedBuild = rootProject.includedBuild("my-included");

        includedBuild.buildGradle().append("""
            println "included build name: ${name}"
            """);

        gradle.withArgs().buildsSuccessfully().assertThat().output().contains("included build name: my-included");
    }

    @Test
    void included_build_with_subproject(GradleInvoker gradle, IncludedBuild myLib) {
        myLib.buildGradle().append("""
            println "included build name: ${name}"
            """);

        SubProject sub = myLib.subproject("sub-module");
        sub.buildGradle().append("""
            println "hello from ${path}"
            """);

        gradle.withArgs().buildsSuccessfully().assertThat().output().contains("included build name: myLib");
        gradle.withArgs().buildsSuccessfully().assertThat().output().contains("hello from :sub-module");
    }

    @Test
    void included_build_settings_gradle(GradleInvoker gradle, IncludedBuild myLib) {
        myLib.settingsGradle().append("""
            println "included settings evaluated"
            """);
        myLib.buildGradle().append("""
            println "included build name: ${name}"
            """);

        gradle.withArgs().buildsSuccessfully().assertThat().output().contains("included settings evaluated");
        gradle.withArgs().buildsSuccessfully().assertThat().output().contains("included build name: myLib");
    }

    @Test
    void included_build_gradle_properties(GradleInvoker gradle, IncludedBuild myLib) {
        myLib.gradlePropertiesFile().setProperty("myProp", "fromIncludedBuild");
        myLib.buildGradle().append("""
            println "myProp: ${findProperty('myProp')}"
            """);

        gradle.withArgs().buildsSuccessfully().assertThat().output().contains("myProp: fromIncludedBuild");
    }

    @Test
    void nested_included_build(GradleInvoker gradle, IncludedBuild outerLib) {
        IncludedBuild innerLib = outerLib.includedBuild("inner");

        innerLib.buildGradle().append("""
            println "inner build name: ${name}"
            """);

        outerLib.buildGradle().append("""
            println "outer build name: ${name}"
            """);

        gradle.withArgs().buildsSuccessfully().assertThat().output().contains("outer build name: outerLib");
        gradle.withArgs().buildsSuccessfully().assertThat().output().contains("inner build name: inner");
    }

    @Test
    void can_request_the_same_included_build_multiple_times(GradleInvoker gradle, RootProject rootProject) {
        IncludedBuild first = rootProject.includedBuild("shared");
        IncludedBuild second = rootProject.includedBuild("shared");

        assertThat(second.path()).isEqualTo(first.path());
        rootProject.settingsGradle().assertThat().content().containsOnlyOnce("includeBuild 'shared'");

        first.buildGradle().append("""
            println "included build name: ${name}"
            """);

        gradle.withArgs().buildsSuccessfully().assertThat().output().contains("included build name: shared");
    }

    @Test
    void subproject_then_included_build_with_same_name_throws(RootProject rootProject) {
        rootProject.subproject("shared");

        assertThatThrownBy(() -> rootProject.includedBuild("shared"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered as a subproject");
    }

    @Test
    void included_build_then_subproject_with_same_name_throws(RootProject rootProject) {
        rootProject.includedBuild("shared");

        assertThatThrownBy(() -> rootProject.subproject("shared"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered as an included build");
    }

    @Nested
    class RootProjectNamePersistence {
        @BeforeEach
        void beforeEach(RootProject rootProject) {
            rootProject.settingsGradle().rootProjectName("something-else");
        }

        @Test
        void root_project_name_should_not_be_reset_to_root_in_test_method(
                GradleInvoker gradle, RootProject rootProject) {
            rootProject.buildGradle().append("""
                println "project name: ${name}"
                """);

            gradle.withArgs().buildsSuccessfully().assertThat().output().contains("project name: something-else");
        }
    }
}
