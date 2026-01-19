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

import com.palantir.gradle.testing.junit.DecoratorContext;
import com.palantir.gradle.testing.junit.GradleInvokerDecorator;
import com.palantir.gradle.testing.junit.GradleInvokerDecoratorRegistry;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface GradleInvoker {

    Logger log = LoggerFactory.getLogger(GradleInvoker.class);

    GradleInvocation withArgs(String... args);

    /**
     * Creates a GradleInvoker with all registered decorators applied.
     *
     * <p>Decorators are applied in registration order: first-registered decorators become
     * innermost wrappers, while later-registered decorators become outer wrappers.
     *
     * @param path the root project directory
     * @param gradleVersion the Gradle version to use
     * @param extensionContext the JUnit extension context containing registered decorators
     * @return a decorated GradleInvoker
     */
    static GradleInvoker create(Path path, GradleVersion gradleVersion, ExtensionContext extensionContext) {
        GradleInvoker invoker = new DefaultGradleInvoker(path, gradleVersion);

        DecoratorContext context = new DecoratorContext(path, gradleVersion, extensionContext);
        List<GradleInvokerDecorator> decorators = GradleInvokerDecoratorRegistry.getDecorators(extensionContext);

        for (GradleInvokerDecorator decorator : decorators) {
            invoker = decorator.decorate(context, invoker);
        }

        return invoker;
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
