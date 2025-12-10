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
import java.nio.file.Path;
import java.util.Arrays;

public record DefaultGradleInvoker(
        Path rootProjectDir, GradleVersion gradleVersion, NamedToolingApiGradleExecutor assignedExecutor)
        implements GradleInvoker {
    @RestrictedApi(explanation = RestrictedCreation.EXPLANATION, allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    public DefaultGradleInvoker {}

    @Override
    public GradleInvocation withArgs(String... args) {
        // ===== EXPERIMENT TOGGLE: Choose your runner implementation =====
        // Option 1: Serial (original GradleRunner) - uncomment to use
        //        GradleRunner runner = GradleRunner.create()
        //                .withProjectDir(rootProjectDir.toFile())
        //                .withDebug(GradleInvoker.shouldRunInTestkitDebugMode())
        //                .forwardOutput()
        //                .withGradleVersion(gradleVersion.version())
        //                .withPluginClasspath()
        //                .withArguments(ImmutableList.<String>builder()
        //                        .addAll(Arrays.asList(args))
        //                        .add("--stacktrace")
        //                        .build());

        // Option 2: DaemonPoolRunner with fixed pool size (old experiment) - uncomment to use
        //        GradleRunner runner = new DaemonPoolRunner(2);
        //        runner = runner.withProjectDir(rootProjectDir.toFile())
        //                .withDebug(GradleInvoker.shouldRunInTestkitDebugMode())
        //                .forwardOutput()
        //                .withGradleVersion(gradleVersion.version())
        //                .withPluginClasspath()
        //                .withArguments(ImmutableList.<String>builder()
        //                        .addAll(Arrays.asList(args))
        //                        .add("--stacktrace")
        //                        .build());

        // Option 3: DaemonPoolRunner with JUnit-level executor assignment + resource locking (NEW EXPERIMENT)
        InjectedGradleRunner runner = new InjectedGradleRunner(assignedExecutor);
        runner = (InjectedGradleRunner) runner.withProjectDir(rootProjectDir.toFile())
                // Use forwardOutputPerTest() instead of forwardOutput() - it writes to System.out
                // in a way that JUnit can properly capture per-test even when tests run in parallel
                .forwardOutput()
                .withDebug(GradleInvoker.shouldRunInTestkitDebugMode())
                .withGradleVersion(gradleVersion.version())
                .withPluginClasspath()
                .withArguments(ImmutableList.<String>builder()
                        .addAll(Arrays.asList(args))
                        .add("--stacktrace")
                        .build());

        return new DefaultGradleInvocation(runner);
    }
}
