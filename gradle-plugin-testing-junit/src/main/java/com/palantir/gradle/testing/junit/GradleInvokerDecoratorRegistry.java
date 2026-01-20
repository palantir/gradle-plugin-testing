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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

/**
 * Registry for {@link GradleInvokerDecorator} instances within the JUnit extension context.
 *
 * <p>Decorators are stored in registration order. When applied, first-registered decorators
 * become innermost wrappers (closest to the base invoker), while later-registered decorators
 * become outer wrappers (executing first in the call chain).
 */
public final class GradleInvokerDecoratorRegistry {
    private static final Namespace NAMESPACE = Namespace.create(GradleInvokerDecoratorRegistry.class);
    private static final String DECORATORS_KEY = "decorators";

    /**
     * Registers a decorator for the given extension context.
     *
     * @param context the JUnit extension context
     * @param decorator the decorator to register
     */
    public static void register(ExtensionContext context, GradleInvokerDecorator decorator) {
        getOrCreateDecoratorList(context).add(decorator);
    }

    /**
     * Returns all registered decorators for the given extension context, in registration order.
     *
     * @param context the JUnit extension context
     * @return an unmodifiable list of decorators in registration order
     */
    public static List<GradleInvokerDecorator> getDecorators(ExtensionContext context) {
        return Optional.ofNullable(getDecoratorList(context))
                .map(List::copyOf)
                .orElseGet(List::of);
    }

    @SuppressWarnings("unchecked")
    private static List<GradleInvokerDecorator> getOrCreateDecoratorList(ExtensionContext context) {
        return (List<GradleInvokerDecorator>)
                context.getStore(NAMESPACE).getOrComputeIfAbsent(DECORATORS_KEY, _key -> new ArrayList<>());
    }

    @SuppressWarnings("unchecked")
    private static List<GradleInvokerDecorator> getDecoratorList(ExtensionContext context) {
        return (List<GradleInvokerDecorator>) context.getStore(NAMESPACE).get(DECORATORS_KEY);
    }

    private GradleInvokerDecoratorRegistry() {}
}
