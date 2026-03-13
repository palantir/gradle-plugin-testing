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

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.Test;

// This is a test fixture, not a real test. It is used to check that CC is disabled even if the annotation is before
// `@GradlePluginTests`.

@GradlePluginTests
public class DisabledConfigurationCacheFixtureTestBefore {

    @Test
    void configuration_cache_is_disabled_by_default(GradleInvoker invoker, RootProject rootProject) {
        rootProject.buildGradle().plugins().add("java");
        rootProject.buildGradle().append("""
            tasks.register("checkConfigurationCache") {
                def buildFeatures = services.get(BuildFeatures)
                def isRequested = buildFeatures.configurationCache.requested.orElse(false)
                inputs.property('configCacheStatus', isRequested)

                doLast {
                    def status = inputs.properties.get('configCacheStatus')
                    println "isConfigurationCacheRequested=" + status
                }
            }
            """);

        // This exception is just so we can pass the output back up to the JUnit testkit-based test that
        // is running this fixture.
        throw new RuntimeException(
                invoker.withArgs("checkConfigurationCache").buildsSuccessfully().output());
    }
}
