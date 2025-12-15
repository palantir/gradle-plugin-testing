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

package com.palantir.gradle.plugintesting;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.palantir.gradle.testing.execution.GradlewInvoker;
import com.palantir.gradle.testing.execution.InvocationResult;
import com.palantir.gradle.testing.junit.DisabledConfigurationCache;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

@GradlePluginTests
@DisabledConfigurationCache
public class GradlewPluginTest {

    private static final Pattern locationPattern = Pattern.compile("Location:\\s+(.*)");
    private static final Pattern languageVersionPattern = Pattern.compile(" Language Version:\\s+(\\d+)");

    @Test
    void javaToolchains_are_correctly_set(GradlewInvoker invoker, RootProject rootProject) {
        rootProject.buildGradle().append("""
            jdks {
                daemonTarget = 21
            }
            """);
        String expectedGradleJdksDir = Path.of("")
                .resolve("build/gradle-plugin-testing/gradle-jdks")
                .toAbsolutePath()
                .toString();
        InvocationResult result = invoker.withArgs("javaToolchains").buildsSuccessfully();
        result.assertThat().output().contains("Auto-detection:     Disabled");
        result.assertThat().output().contains("Auto-download:      Disabled");
        Matcher matcher = locationPattern.matcher(result.output());
        while (matcher.find()) {
            assertThat(matcher.group(1)).startsWith(expectedGradleJdksDir);
        }
    }

    @Test
    void baselineJavaVersions_are_correctly_set(GradlewInvoker invoker, RootProject rootProject) {
        rootProject.buildGradle().plugins().add("com.palantir.baseline-java-versions");
        rootProject.buildGradle().append("""
            javaVersions {
                libraryTarget = '11'
            }

            jdks {
                daemonTarget = 21
            }
            """);

        InvocationResult result = invoker.withArgs("javaToolchains").buildsSuccessfully();
        Matcher matcher = languageVersionPattern.matcher(result.output());
        ImmutableList.Builder<String> versionsBuilder = ImmutableList.builder();
        while (matcher.find()) {
            versionsBuilder.add(matcher.group(1));
        }
        List<String> versions = versionsBuilder.build();
        assertThat(versions).contains("11", "21");
    }
}
