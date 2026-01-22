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

package com.palantir.gradle.testing.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit extension that discovers annotations with {@link RegistersGradleInvokerDecorator}
 * and automatically registers the corresponding decorators.
 *
 * <p>This extension scans the test class and method for annotations that have the
 * {@code @RegistersGradleInvokerDecorator} meta-annotation, instantiates the specified factory,
 * and registers the created decorator.
 *
 * Throws an IllegalStateException if the same discovered annotation is duplicated from one of the parent classes.
 */
public final class GradleInvokerDecoratorDiscoveryExtension implements BeforeAllCallback, BeforeEachCallback {

    private static final Logger log = LoggerFactory.getLogger(GradleInvokerDecoratorDiscoveryExtension.class);

    @Override
    public void beforeAll(ExtensionContext context) {
        Class<?> currentClass = context.getRequiredTestClass();

        Set<Class<? extends Annotation>> currentDiscoveredDecorators = discoverDecorators(currentClass);

        throwForDuplicatedAnnotations(
                context, currentDiscoveredDecorators, String.format("""
                    The same decorator annotation cannot be applied at multiple class levels. Please remove the extra annotation from the class `%s`.
                    """, currentClass.getSimpleName()));

        Set<Class<? extends Annotation>> allClassLevelDecorators = Stream.concat(
                        getParentDecorators(context).stream(), currentDiscoveredDecorators.stream())
                .collect(Collectors.toSet());
        DiscoveredDecoratorsByClassStore.storeDecorators(context, allClassLevelDecorators);

        registerDecorators(context, currentClass);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        if (context.getTestMethod().isEmpty()) {
            return;
        }

        Set<Class<? extends Annotation>> methodDecorators =
                discoverDecorators(context.getTestMethod().get());

        throwForDuplicatedAnnotations(
                context,
                methodDecorators,
                String.format("""
                    The same decorator annotation cannot be applied at both class and method level. Please remove it from the test method `%s`.
                    """, context.getTestMethod().get().getName()));

        registerDecorators(context, context.getTestMethod().get());
    }

    private void registerDecorators(ExtensionContext context, AnnotatedElement element) {
        Stream.of(element.getAnnotations()).forEach(annotation -> Optional.ofNullable(
                        annotation.annotationType().getAnnotation(RegistersGradleInvokerDecorator.class))
                .ifPresent(registersGradleInvokerDecorator ->
                        registerDecoratorFromAnnotation(context, annotation, registersGradleInvokerDecorator)));
    }

    @SuppressWarnings("unchecked")
    private void registerDecoratorFromAnnotation(
            ExtensionContext context, Annotation annotation, RegistersGradleInvokerDecorator registeringDecorator) {
        try {
            Class<? extends GradleInvokerDecoratorFactory<?>> factoryClass = registeringDecorator.value();
            GradleInvokerDecoratorFactory<Annotation> factory = (GradleInvokerDecoratorFactory<Annotation>)
                    factoryClass.getDeclaredConstructor().newInstance();

            GradleInvokerDecorator decorator = factory.create(annotation);
            GradleInvokerDecoratorRegistry.register(context, decorator);

            log.debug(
                    "Registered decorator from @{}: {}",
                    annotation.annotationType().getSimpleName(),
                    decorator.getClass().getSimpleName());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(
                    String.format(
                            "Failed to instantiate decorator factory %s for annotation @%s",
                            registeringDecorator.value().getName(),
                            annotation.annotationType().getSimpleName()),
                    e);
        }
    }

    private static void throwForDuplicatedAnnotations(
            ExtensionContext context, Set<Class<? extends Annotation>> currentDiscoveredDecorators, String message) {
        Set<Class<? extends Annotation>> parentDecorators = getParentDecorators(context);
        currentDiscoveredDecorators.stream()
                .filter(parentDecorators::contains)
                .findFirst()
                .ifPresent(decorator -> {
                    throw new IllegalStateException(String.format(
                            "Decorator annotation @%s is already registered in a parent class. %s",
                            decorator.getSimpleName(), message));
                });
    }

    private static Set<Class<? extends Annotation>> getParentDecorators(ExtensionContext context) {
        return context.getParent()
                .map(enclosingClass -> DiscoveredDecoratorsByClassStore.getStoredDecorators(context))
                .orElseGet(Set::of);
    }

    /**
     * Discovers decorator annotation types from an annotated element without registering them.
     */
    private Set<Class<? extends Annotation>> discoverDecorators(AnnotatedElement element) {
        return Stream.of(element.getAnnotations())
                .map(Annotation::annotationType)
                .filter(annotationType -> Optional.ofNullable(
                                annotationType.getAnnotation(RegistersGradleInvokerDecorator.class))
                        .isPresent())
                .collect(Collectors.toSet());
    }
}
