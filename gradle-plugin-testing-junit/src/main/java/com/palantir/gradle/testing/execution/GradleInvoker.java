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
import com.palantir.gradle.testing.junit.DecoratorContext;
import com.palantir.gradle.testing.junit.GradleInvokerDecorator;
import com.palantir.gradle.testing.junit.RegistersGradleInvokerDecorator;
import com.palantir.gradle.testing.project.RootProject;
import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

public interface GradleInvoker {

    GradleInvocation withArgs(String... args);

    static GradleInvoker create(Path path, GradleVersion gradleVersion, ExtensionContext extensionContext) {
        GradleInvoker baseInvoker = new DefaultGradleInvoker(path, gradleVersion);
        RootProject rootProject = new RootProject(path);
        DecoratorContext decoratorContext = new DecoratorContext(rootProject, gradleVersion, extensionContext);
        Set<Annotation> annotations = collectAnnotationsFromContext(extensionContext);
        return decorateInvokerWithAnnotations(decoratorContext, baseInvoker, annotations);
    }

    private static Set<Annotation> collectAnnotationsFromContext(ExtensionContext context) {
        List<Annotation> methodAnnotations = context.getTestMethod()
                .map(GradleInvoker::findAllAnnotationsWithRegisterDecorator)
                .orElseGet(List::of);
        // Collect contexts from bottom to top (method -> class -> parent classes)
        List<ExtensionContext> contextHierarchy = Stream.iterate(
                        context, Objects::nonNull, ctx -> ctx.getParent().orElse(null))
                .collect(Collectors.toList());

        // Reverse to get top-down order (parent classes -> class -> method)
        Collections.reverse(contextHierarchy);

        Stream<Annotation> classAnnotations = contextHierarchy.stream()
                .flatMap(ctx -> ctx.getTestClass().stream())
                .flatMap(testClass -> findAllAnnotationsWithRegisterDecorator(testClass).stream());

        return Stream.concat(classAnnotations, methodAnnotations.stream())
                // preserving the order while dropping duplicated annotations
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static List<Annotation> findAllAnnotationsWithRegisterDecorator(AnnotatedElement element) {
        ImmutableList.Builder<Annotation> annotationBuilder = ImmutableList.builder();

        for (Annotation annotation : element.getAnnotations()) {
            if (AnnotationSupport.isAnnotated(annotation.annotationType(), RegistersGradleInvokerDecorator.class)) {
                annotationBuilder.add(annotation);
            }
            // Check if this might be a container annotation for repeatable annotations
            // Look for annotations that might be containers for @RegistersGradleInvokerDecorator annotations
            try {
                Method valueMethod = annotation.annotationType().getDeclaredMethod("value");
                if (valueMethod.getReturnType().isArray()) {
                    Class<?> componentType = valueMethod.getReturnType().getComponentType();

                    if (Annotation.class.isAssignableFrom(componentType)
                            && componentType.isAnnotationPresent(RegistersGradleInvokerDecorator.class)) {
                        Object value = valueMethod.invoke(annotation);
                        if (value instanceof Annotation[] containedAnnotations) {
                            annotationBuilder.addAll(
                                    Arrays.stream(containedAnnotations).toList());
                        }
                    }
                }
            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                // not a repeatable annotation, ignore this
            }
        }
        return annotationBuilder.build();
    }

    /**
     * Groups annotations by their decorator class and decorates the baseInvoker passing the relevant
     * `@RegistersGradleInvokerDecorator` annotations.
     */
    private static GradleInvoker decorateInvokerWithAnnotations(
            DecoratorContext context, GradleInvoker baseInvoker, Set<Annotation> annotations) {
        Set<? extends Class<? extends GradleInvokerDecorator>> decoratorClasses = annotations.stream()
                .map(annotation -> Optional.ofNullable(
                        annotation.annotationType().getAnnotation(RegistersGradleInvokerDecorator.class)))
                .<RegistersGradleInvokerDecorator>mapMulti(Optional::ifPresent)
                .map(RegistersGradleInvokerDecorator::value)
                // maintains the original order, while dropping duplicates
                .collect(Collectors.toCollection(LinkedHashSet::new));

        GradleInvoker invoker = baseInvoker;
        for (Class<? extends GradleInvokerDecorator> decoratorClass : decoratorClasses) {
            invoker = createGradleInvokerFromDecorator(decoratorClass, context, invoker, annotations);
        }
        return invoker;
    }

    @SuppressWarnings("unchecked")
    private static GradleInvoker createGradleInvokerFromDecorator(
            Class<? extends GradleInvokerDecorator> decoratorClass,
            DecoratorContext context,
            GradleInvoker invoker,
            Set<Annotation> annotations) {
        try {
            // Get decorator's generic type parameter (the annotation type it can process)
            Class<? extends Annotation> annotationType = getDecoratorAnnotationType(decoratorClass);

            // Filter annotations to only include those of the expected type
            List<Annotation> filteredAnnotations =
                    annotations.stream().filter(annotationType::isInstance).toList();

            GradleInvokerDecorator<Annotation> decorator =
                    decoratorClass.getDeclaredConstructor().newInstance();
            return decorator.decorate(context, invoker, filteredAnnotations);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(
                    String.format("Failed to instantiate decorator class %s", decoratorClass.getSimpleName()), e);
        }
    }

    /**
     * Determines the annotation type that a decorator can process by examining its generic type parameter.
     */
    @SuppressWarnings("unchecked")
    private static Class<? extends Annotation> getDecoratorAnnotationType(
            Class<? extends GradleInvokerDecorator> decoratorClass) {
        // Look for the GradleInvokerDecorator interface in the class hierarchy
        Type[] genericInterfaces = decoratorClass.getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType parameterizedType) {
                if (GradleInvokerDecorator.class.equals(parameterizedType.getRawType())) {
                    Type typeArg = parameterizedType.getActualTypeArguments()[0];
                    if (typeArg instanceof Class<?> annotationClass) {
                        return (Class<? extends Annotation>) annotationClass;
                    }
                }
            }
        }

        // If we couldn't find it directly, check the superclass
        Class<?> superclass = decoratorClass.getSuperclass();
        if (superclass != null && GradleInvokerDecorator.class.isAssignableFrom(superclass)) {
            return getDecoratorAnnotationType((Class<? extends GradleInvokerDecorator>) superclass);
        }

        // we shouldn't reach here
        throw new IllegalStateException(String.format(
                "Could not determine annotation type for decorator class %s", decoratorClass.getSimpleName()));
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
