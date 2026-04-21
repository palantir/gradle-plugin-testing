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
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.errorprone.annotations.RestrictedApi;
import com.palantir.gradle.plugintesting.GradleDistributionBaseUrl;
import com.palantir.gradle.testing.RestrictedCreation;
import com.palantir.gradle.testing.junit.DecoratorContext;
import com.palantir.gradle.testing.junit.GradleInvokerDecorator;
import com.palantir.gradle.testing.junit.RegistersGradleInvokerDecorator;
import com.palantir.gradle.testing.project.RootProject;
import com.palantir.gradle.testing.project.TopLevelRootProject;
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

public interface GradleInvoker {

    default GradleInvocation withArgs(String... args) {
        return with(Options.builder().args(Arrays.asList(args)).build());
    }

    GradleInvocation with(Options options);

    static GradleInvoker create(Path path, GradleVersion gradleVersion, ExtensionContext extensionContext) {
        String baseUrl = readGradleDistributionBaseUrl(extensionContext);
        GradleInvoker baseInvoker = getInternalDefaultInvoker(path, gradleVersion, baseUrl);
        RootProject rootProject = new TopLevelRootProject(path);
        DecoratorContext decoratorContext = new DecoratorContext(rootProject, gradleVersion, extensionContext);
        Set<Annotation> annotations = collectAnnotationsFromContext(extensionContext);
        return decorateInvokerWithAnnotations(decoratorContext, baseInvoker, annotations);
    }

    static String readGradleDistributionBaseUrl(ExtensionContext extensionContext) {
        return extensionContext
                .getConfigurationParameter(GradleDistributionBaseUrl.GRADLE_DISTRIBUTION_BASE_URL_SYSTEM_PROPERTY)
                .orElseThrow(() -> new RuntimeException(
                        "Could not determine the Gradle distribution base URL. Have you applied the latest"
                                + " `com.palantir.gradle-plugin-testing` plugin to this project?"));
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
                if (!valueMethod.getReturnType().isArray()) {
                    continue;
                }
                Class<?> componentType = valueMethod.getReturnType().getComponentType();
                if (!Annotation.class.isAssignableFrom(componentType)) {
                    continue;
                }
                if (!componentType.isAnnotationPresent(RegistersGradleInvokerDecorator.class)) {
                    continue;
                }
                Object value = valueMethod.invoke(annotation);
                if (value instanceof Annotation[] containedAnnotations) {
                    annotationBuilder.add(containedAnnotations);
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

        ListMultimap<Class<? extends GradleInvokerDecorator>, Annotation> decoratorToAnnotations =
                MultimapBuilder.linkedHashKeys().arrayListValues().build();
        for (Annotation annotation : annotations) {
            RegistersGradleInvokerDecorator registersMeta =
                    annotation.annotationType().getAnnotation(RegistersGradleInvokerDecorator.class);
            if (registersMeta == null) {
                continue;
            }
            Class<? extends GradleInvokerDecorator> decoratorClass = registersMeta.value();
            decoratorToAnnotations.put(decoratorClass, annotation);
        }

        // Apply decorators in the original order, passing only relevant annotations to each
        GradleInvoker invoker = baseInvoker;
        for (Class<? extends GradleInvokerDecorator> decoratorClass : decoratorToAnnotations.keySet()) {
            invoker = createGradleInvokerFromDecorator(
                    context, invoker, decoratorClass, decoratorToAnnotations.get(decoratorClass));
        }
        return invoker;
    }

    @SuppressWarnings("unchecked")
    private static GradleInvoker createGradleInvokerFromDecorator(
            DecoratorContext context,
            GradleInvoker invoker,
            Class<? extends GradleInvokerDecorator> decoratorClass,
            List<Annotation> annotations) {
        try {
            checkAnnotationsType(decoratorClass, annotations);

            GradleInvokerDecorator<Annotation> decorator =
                    decoratorClass.getDeclaredConstructor().newInstance();
            return decorator.decorate(context, invoker, annotations);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(
                    String.format("Failed to instantiate decorator class %s", decoratorClass.getSimpleName()), e);
        }
    }

    private static void checkAnnotationsType(
            Class<? extends GradleInvokerDecorator> decoratorClass, List<Annotation> annotations) {
        Class<?> expectedAnnotationType = getDecoratorAnnotationType(decoratorClass);
        List<String> incompatibleAnnotations = annotations.stream()
                .filter(annotation -> !expectedAnnotationType.isInstance(annotation))
                .map(annotation -> annotation.annotationType().getSimpleName())
                .toList();
        if (!incompatibleAnnotations.isEmpty()) {
            throw new RuntimeException(String.format(
                    "Type mismatch: Decorator %s expects annotations of type %s, but received incompatible annotation"
                            + " types: %s",
                    decoratorClass.getSimpleName(), expectedAnnotationType.getSimpleName(), incompatibleAnnotations));
        }
    }

    /**
     * Extracts the annotation type parameter by examining the decorate method's 3rd param type `A`:
     * {@code decorate(
     *      DecoratorContext _firstParam, GradleInvoker _secondParam, List{@literal <}A{@literal >} thirdParam)}
     */
    private static Class<?> getDecoratorAnnotationType(Class<?> decoratorClass) {
        try {
            Method decorateMethod =
                    decoratorClass.getMethod("decorate", DecoratorContext.class, GradleInvoker.class, List.class);
            Type[] genericParameterTypes = decorateMethod.getGenericParameterTypes();
            if (genericParameterTypes.length >= 3 && genericParameterTypes[2] instanceof ParameterizedType paramType) {
                Type[] typeArgs = paramType.getActualTypeArguments();
                if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                    return (Class<?>) typeArgs[0];
                }
            }
            throw new RuntimeException(String.format(
                    "Could not determine expected annotation type for decorator %s", decoratorClass.getSimpleName()));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(
                    String.format(
                            "Could not determine expected annotation type for decorator %s",
                            decoratorClass.getSimpleName()),
                    e);
        }
    }

    @RestrictedApi(
            explanation =
                    "For internal use only. Always prefer GradleInvoker#create for the creation of the GradleInvokers.",
            allowedOnPath = RestrictedCreation.ALLOWED_ON_PATH)
    static GradleInvoker getInternalDefaultInvoker(Path path, GradleVersion gradleVersion, String baseUrl) {
        return new DefaultGradleInvoker(path, gradleVersion, baseUrl);
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
