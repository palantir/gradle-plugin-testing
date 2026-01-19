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

import com.palantir.gradle.testing.execution.GradleInvoker;

/**
 * A decorator that wraps a {@link GradleInvoker} to add additional behavior.
 *
 * <p>Decorators are applied in registration order. First registered decorators become
 * the innermost wrappers (closest to the base invoker), while later registered decorators
 * become outer wrappers (executing first).
 *
 * <p>To create a decorator that is automatically registered via annotation, implement
 * {@link GradleInvokerDecoratorFactory} and use the {@link RegistersGradleInvokerDecorator}
 * meta-annotation on your test annotation.
 */
public interface GradleInvokerDecorator {

    /**
     * Decorates the given invoker.
     *
     * @param context provides access to the root project directory, Gradle version, and JUnit extension context
     * @param delegate the invoker to decorate
     * @return the decorated invoker
     */
    GradleInvoker decorate(DecoratorContext context, GradleInvoker delegate);
}
