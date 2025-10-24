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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@GradlePluginTests
public class BeforeEachUsageTest {

    @BeforeEach
    void beforeEach(RootProject rootProject) {
        rootProject
                .settingsGradle()
                .prependLine("plugins { id 'org.gradle.toolchains.foojay-resolver-convention' version '0.8.0' }");
    }

    @Test
    void can_prepend_in_before_each(GradleInvoker gradle, RootProject serviceProject) {
        serviceProject.buildGradle().append("""
            println "hello from ${path}"
            """);

        assertThat(gradle.withArgs().buildsSuccessfully().output()).contains("hello from :");
    }
}
