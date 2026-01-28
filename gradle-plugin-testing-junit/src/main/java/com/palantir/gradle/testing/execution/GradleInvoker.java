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

import com.palantir.gradle.testing.junit.ConfigurationCacheStore;
import com.palantir.gradle.testing.junit.DecoratorContext;
import com.palantir.gradle.testing.junit.GradleInvokerDecorator;
import com.palantir.gradle.testing.junit.GradleInvokerDecoratorFactory;
import com.palantir.gradle.testing.junit.RegistersGradleInvokerDecorator;
import com.palantir.gradle.testing.project.RootProject;
import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface GradleInvoker {

    Logger log = LoggerFactory.getLogger(GradleInvoker.class);

    GradleInvocation withArgs(String... args);

    /**
     * Creates a GradleInvoker with all discovered decorators applied.
     * Decorators are collected from the test extension context hierarchy, starting from the test method
     * and moving up through parent classes. They are applied in registration order: first-registered decorators become
     * innermost wrappers, while later-registered decorators become outer wrappers.
     */
    static GradleInvoker create(Path path, GradleVersion gradleVersion, ExtensionContext extensionContext) {
        GradleInvoker baseInvoker = new DefaultGradleInvoker(path, gradleVersion);
        RootProject rootProject = new RootProject(path);
        DecoratorContext decoratorContext = new DecoratorContext(rootProject, gradleVersion, extensionContext);
        List<GradleInvokerDecorator> decorators = discoverDecorators(extensionContext);
        for (GradleInvokerDecorator decorator : decorators) {
            baseInvoker = decorator.decorate(decoratorContext, baseInvoker);
        }
        return baseInvoker;
    }

    private static List<GradleInvokerDecorator> discoverDecorators(ExtensionContext extensionContext) {
        List<GradleInvokerDecorator> inheritedAnnotations = collectAnnotationsFromContext(extensionContext).stream()
                .map(GradleInvoker::getDecoratorFromAnnotation)
                .toList();
        List<GradleInvokerDecorator> extraAnnotations =
                ConfigurationCacheStore.isConfigurationCacheEnabled(extensionContext)
                        ? List.of(new ConfigurationCacheDecorator())
                        : List.of();
        return Stream.concat(inheritedAnnotations.stream(), extraAnnotations.stream())
                .toList();
    }

    /**
     * Collects all unique annotations starting from the current extension context (test method)
     * and going up the parent tree (test class, parent classes, etc.).
     */
    private static Set<Annotation> collectAnnotationsFromContext(ExtensionContext context) {
        return Stream.iterate(context, Objects::nonNull, ctx -> ctx.getParent().orElse(null))
                .flatMap(ctx -> Stream.concat(
                        ctx.getTestMethod()
                                .map(method -> getAnnotations(method.getAnnotations()))
                                .orElseGet(Stream::empty),
                        ctx.getTestClass()
                                .map(clazz -> getAnnotations(clazz.getAnnotations()))
                                .orElseGet(Stream::empty)))
                .collect(Collectors.toSet());
    }

    private static Stream<Annotation> getAnnotations(Annotation[] annotations) {
        return Stream.of(annotations)
                .filter(annotation ->
                        annotation.annotationType().isAnnotationPresent(RegistersGradleInvokerDecorator.class));
    }

    @SuppressWarnings("unchecked")
    private static GradleInvokerDecorator getDecoratorFromAnnotation(Annotation annotation) {
        try {
            RegistersGradleInvokerDecorator registersDecorator =
                    annotation.annotationType().getAnnotation(RegistersGradleInvokerDecorator.class);

            if (registersDecorator == null) {
                throw new IllegalArgumentException("Annotation "
                        + annotation.annotationType().getName() + " does not have @RegistersGradleInvokerDecorator");
            }

            Class<? extends GradleInvokerDecoratorFactory<?>> factoryClass = registersDecorator.value();
            GradleInvokerDecoratorFactory<Annotation> factory = (GradleInvokerDecoratorFactory<Annotation>)
                    factoryClass.getDeclaredConstructor().newInstance();

            GradleInvokerDecorator decorator = factory.create(annotation);
            log.debug(
                    "Found decorator from @{}: {}",
                    annotation.annotationType().getSimpleName(),
                    decorator.getClass().getSimpleName());
            return decorator;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(
                    String.format(
                            "Failed to instantiate decorator factory for annotation @%s",
                            annotation.annotationType().getSimpleName()),
                    e);
        }
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
