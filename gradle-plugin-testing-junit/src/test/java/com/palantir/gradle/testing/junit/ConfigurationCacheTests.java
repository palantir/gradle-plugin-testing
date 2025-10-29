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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.execution.InvocationResult;
import com.palantir.gradle.testing.execution.TaskOutcome;
import com.palantir.gradle.testing.execution.UnexpectedConfigurationCacheFailure;
import com.palantir.gradle.testing.execution.UnexpectedInvocationSuccess;
import com.palantir.gradle.testing.project.RootProject;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

@GradlePluginTests
@WithConfigurationCache
class ConfigurationCacheTests {

    @Test
    void configuration_cache_is_enabled(GradleInvoker invoker, RootProject rootProject) {
        rootProject.buildGradle().append("""
            plugins { id 'java' }
            def isConfigurationCacheRequested = services.get(BuildFeatures).configurationCache.requested.orElse(false).get()

            tasks.register("checkConfigurationCache") {
                doLast {
                    println "isConfigurationCacheRequested=" + isConfigurationCacheRequested
                }
            }
            """);

        InvocationResult result = invoker.withArgs("checkConfigurationCache").buildsSuccessfully();
        assertThat(result.output())
                .contains("isConfigurationCacheRequested=true")
                .contains("Configuration cache entry stored.");
        assertThat(result.task(":checkConfigurationCache")).hasValueSatisfying(taskResult -> {
            assertThat(taskResult.outcome()).isEqualTo(TaskOutcome.SUCCESS);
        });

        InvocationResult secondResult =
                invoker.withArgs("checkConfigurationCache").buildsSuccessfully();
        assertThat(secondResult.output())
                .contains("isConfigurationCacheRequested=true")
                .contains("Configuration cache entry reused.");
    }

    @Test
    void fails_configuration_cache_for_external_process(GradleInvoker invoker, RootProject rootProject) {
        rootProject.buildGradle().append("""
            plugins { id 'java' }

            def output = ['echo', 'Hello from config time!'].execute().text.trim()
            println "External command output: $output"
            """);
        assertThatThrownBy(() -> invoker.withArgs("help").buildsSuccessfully())
                .isInstanceOf(UnexpectedConfigurationCacheFailure.class)
                .hasMessageContaining(
                        "Unexpected build execution failure. Build Execution failure caused by configuration cache"
                                + " incompatibility.");

        assertThatThrownBy(() -> invoker.withArgs("help").buildsWithFailure())
                .isInstanceOf(UnexpectedConfigurationCacheFailure.class)
                .hasMessageContaining(
                        "The GradleInvocation was run with configuration cache enabled. Expected configuration cache"
                                + " entry to be stored, but it wasn't.");
    }

    @Test
    void fails_when_configuration_cache_cannot_be_loaded(GradleInvoker invoker, RootProject rootProject)
            throws IOException {
        rootProject.buildGradle().append("""
            plugins { id 'java' }

            tasks.register("deleteConfigCache", Delete) {
                delete(layout.projectDirectory.dir(".gradle/configuration-cache"))
            }
            """);

        assertThatThrownBy(() -> invoker.withArgs("deleteConfigCache").buildsSuccessfully())
                .isInstanceOf(UnexpectedConfigurationCacheFailure.class)
                .hasMessageContaining("The GradleInvocation was run with configuration cache enabled.")
                .hasMessageContaining(
                        "Calculating task graph as no cached configuration is available for tasks: deleteConfigCache");

        FileUtils.deleteDirectory(
                rootProject.path().resolve(".gradle/configuration-cache").toFile());
        assertThatThrownBy(() -> invoker.withArgs("deleteConfigCache").buildsWithFailure())
                .isInstanceOf(UnexpectedInvocationSuccess.class)
                .hasMessageContaining("Configuration cache entry stored.");
    }

    @Test
    void fails_when_configuration_cache_cannot_be_stored(GradleInvoker invoker, RootProject rootProject) {
        rootProject.buildGradle().append("""
            plugins { id 'java' }
            class BadConfigCacheTask extends DefaultTask {
                @Internal
                Object nonSerializable = new Thread() // Thread is not serializable

                @TaskAction
                void run() {
                    println "Task ran!"
                }
            }

            tasks.register('badConfigCache', BadConfigCacheTask) {
                nonSerializable = new Thread()
            }
            """);

        assertThatThrownBy(() -> invoker.withArgs("badConfigCache").buildsSuccessfully())
                .isInstanceOf(UnexpectedConfigurationCacheFailure.class)
                .hasMessageContaining(
                        "Unexpected build execution failure. Build Execution failure caused by configuration cache"
                                + " incompatibility.")
                .hasMessageContaining("1 problem was found storing the configuration cache.");
    }
}
