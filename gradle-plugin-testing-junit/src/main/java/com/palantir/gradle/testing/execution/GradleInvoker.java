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

import com.google.errorprone.annotations.RestrictedApi;
import com.palantir.gradle.testing.RestrictedCreation;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface GradleInvoker {

    Logger log = LoggerFactory.getLogger(GradleInvoker.class);

    default GradleInvocation withArgs(String... args) {
        return with(Options.builder().args(Arrays.asList(args)).build());
    }

    GradleInvocation with(Options options);

    static GradleInvoker create(Path path, GradleVersion gradleVersion, boolean configurationCache) {
        GradleInvoker gradleInvoker = getInternalDefaultInvoker(path, gradleVersion);
        if (configurationCache) {
            if (shouldRunInTestkitDebugMode()) {
                log.warn("Configuration cache disabled because debug mode is active. Debug mode and"
                        + " configuration cache cannot be used together. See"
                        + " https://github.com/gradle/gradle/issues/25846 for details.");
                return gradleInvoker;
            }
            return new ConfigurationCacheInvoker(path, gradleInvoker);
        }
        return gradleInvoker;
    }

    @RestrictedApi(
            explanation =
                    "For internal use only. Always prefer GradleInvoker#create for the creation of the GradleInvokers.",
            allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    static GradleInvoker getInternalDefaultInvoker(Path path, GradleVersion gradleVersion) {
        return new DefaultGradleInvoker(path, gradleVersion);
    }

    static boolean shouldRunInTestkitDebugMode() {
        // `withDebug(true)` will run the Gradle daemon inside the same JVM as the test, whereas
        // `withDebug(false)` will run Gradle in a new daemon.
        // When running tests from IntelliJ with debug or coverage, they only work when the Gradle daemon
        // is in the same the JVM as the test, so we must set `withDebug(true)` in these cases.
        // Beware: There can be differences between these two modes!
        return isJavaDebugAgentLoaded() || isRunningCoverageInIntelliJ();
    }

    private static boolean isJavaDebugAgentLoaded() {
        // When you run a test with debug in intellij, it passes an arg to the test process like:
        //   -agentlib:jdwp=transport=dt_socket,server=n,suspend=y,address=127.0.0.1:54342
        return ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .anyMatch(arg -> arg.contains("-agentlib:jdwp"));
    }

    private static boolean isRunningCoverageInIntelliJ() {
        // When you run a test with coverage in intellij, it sets a system property on the test JVM
        // by adding the jvm arg `-Didea.coverage.calculate.hits=true`.
        return Boolean.getBoolean("idea.coverage.calculate.hits");
    }
}
