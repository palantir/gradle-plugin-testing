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

package com.palantir.gradle.testing.execution;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.RestrictedApi;
import com.palantir.gradle.testing.RestrictedCreation;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.Arrays;
import org.gradle.testkit.runner.GradleRunner;

public record DefaultGradleInvoker(Path rootProjectDir, GradleVersion gradleVersion) implements GradleInvoker {
    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public DefaultGradleInvoker {}

    @Override
    public GradleInvocation withArgs(String... args) {
        String[] argsWithStacktrace = ImmutableList.builder()
                .addAll(Arrays.asList(args))
                .add("--stacktrace")
                .build()
                .toArray(String[]::new);
        GradleRunner runner = GradleRunner.create()
                .withProjectDir(rootProjectDir.toFile())
                .withDebug(isJavaDebugAgentLoaded())
                .forwardOutput()
                .withGradleVersion(gradleVersion.version())
                .withPluginClasspath()
                .withArguments(argsWithStacktrace);

        return new DefaultGradleInvocation(runner);
    }

    private static boolean isJavaDebugAgentLoaded() {
        // When you run a test with debug in intellij, it passes an arg to the test process like:
        //   -agentlib:jdwp=transport=dt_socket,server=n,suspend=y,address=127.0.0.1:54342
        // We can use this to detect whether we should run with Gradle tooling `.withDebug` or not,
        // `withDebug(true)` will run the Gradle tooling inside the same JVM as the test, meaning
        // debugging works, whereas `withDebug(false)` will run Gradle in a new daemon. There can be
        // differences between these two modes!
        return ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .anyMatch(s -> s.contains("-agentlib:jdwp"));
    }
}
