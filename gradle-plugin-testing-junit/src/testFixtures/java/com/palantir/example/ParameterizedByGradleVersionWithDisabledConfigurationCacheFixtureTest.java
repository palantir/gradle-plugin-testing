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
import com.palantir.gradle.testing.junit.DisabledConfigurationCache;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.junit.InjectByGradleVersion;
import com.palantir.gradle.testing.junit.ParameterizedByGradleVersion;
import com.palantir.gradle.testing.junit.WhenVersion;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.Test;

/**
 * Fixture test for verifying ParameterizedByGradleVersion works with @DisabledConfigurationCache.
 *
 * <p>This test verifies that:
 * 1. Tests run with configuration cache disabled
 * 2. ParameterizedByGradleVersion values are correctly resolved
 */
@GradlePluginTests
public final class ParameterizedByGradleVersionWithDisabledConfigurationCacheFixtureTest {

    @Test
    @ParameterizedByGradleVersion(when = @WhenVersion(lessThan = "8.0", stringValue = "old"), otherwiseString = "new")
    @DisabledConfigurationCache("Testing compatibility with @ParameterizedByGradleVersion")
    void test_with_parameter(
            GradleInvoker gradleInvoker, RootProject rootProject, @InjectByGradleVersion String behavior) {
        rootProject.buildGradle().append("""
            import org.gradle.util.GradleVersion

            tasks.register('checkConfigurationCache') {
                def configCacheEnabled = gradle.startParameter.configurationCacheRequested
                inputs.property('configCacheStatus', configCacheEnabled)

                doLast {
                    def status = inputs.properties.get('configCacheStatus')
                    println "isConfigurationCacheRequested=" + status
                }
            }

            println "GradleVersion: ${GradleVersion.current().version}"
            println "Behavior: %s"
            """, behavior);

        String output = gradleInvoker
                .withArgs("checkConfigurationCache")
                .buildsSuccessfully()
                .output();
        throw new RuntimeException("behavior=" + behavior + "|output=" + output);
    }
}
