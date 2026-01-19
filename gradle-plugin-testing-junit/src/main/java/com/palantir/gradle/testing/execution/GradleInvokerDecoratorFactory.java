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

import java.lang.annotation.Annotation;

/**
 * Factory for creating {@link GradleInvokerDecorator} instances from annotations.
 *
 * <p>Implementations are referenced by the {@link RegistersGradleInvokerDecorator} meta-annotation.
 * When a test class or method is annotated with an annotation that has {@code @RegistersGradleInvokerDecorator},
 * the framework will instantiate the factory and call {@link #create} with the annotation instance.
 *
 * <p>Factories must have a public no-argument constructor.
 *
 * @param <A> the annotation type this factory handles
 */
public interface GradleInvokerDecoratorFactory<A extends Annotation> {

    /**
     * Creates a decorator from the given annotation.
     *
     * @param annotation the annotation instance from the test class or method
     * @return a decorator that will be registered for this test
     */
    GradleInvokerDecorator create(A annotation);
}
