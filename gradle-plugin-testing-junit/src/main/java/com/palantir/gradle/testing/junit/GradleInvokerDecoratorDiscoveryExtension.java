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
 */
public final class GradleInvokerDecoratorDiscoveryExtension implements BeforeAllCallback, BeforeEachCallback {

    private static final Logger log = LoggerFactory.getLogger(GradleInvokerDecoratorDiscoveryExtension.class);

    @Override
    public void beforeAll(ExtensionContext context) {
        discoverAndRegisterDecorators(context, context.getRequiredTestClass());
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        context.getTestMethod().ifPresent(method -> discoverAndRegisterDecorators(context, method));
    }

    private void discoverAndRegisterDecorators(ExtensionContext context, AnnotatedElement element) {
        for (Annotation annotation : element.getAnnotations()) {
            Optional.ofNullable(annotation.annotationType().getAnnotation(RegistersGradleInvokerDecorator.class))
                    .ifPresent(registersGradleInvokerDecorator ->
                            registerDecoratorFromAnnotation(context, annotation, registersGradleInvokerDecorator));
        }
    }

    @SuppressWarnings("unchecked")
    private void registerDecoratorFromAnnotation(
            ExtensionContext context, Annotation annotation, RegistersGradleInvokerDecorator meta) {
        try {
            Class<? extends GradleInvokerDecoratorFactory<?>> factoryClass = meta.value();
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
                            meta.value().getName(), annotation.annotationType().getSimpleName()),
                    e);
        }
    }
}
