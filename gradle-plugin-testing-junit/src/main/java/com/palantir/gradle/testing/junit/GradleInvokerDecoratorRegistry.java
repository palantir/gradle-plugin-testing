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
     * Decorates a GradleInvoker with all registered decorators for the given extension context.
     *
     * <p>Decorators are applied in registration order: first-registered decorators become
     * innermost wrappers (closest to the base invoker), while later-registered decorators
     * become outer wrappers (executing first in the call chain).
     *
     * @param context the JUnit extension context
     * @param decoratorContext the decorator context containing test metadata
     * @param invoker the base invoker to decorate
     * @return the decorated invoker with all registered decorators applied
     */
    public static GradleInvoker decorate(
            ExtensionContext context, DecoratorContext decoratorContext, GradleInvoker invoker) {
        List<GradleInvokerDecorator> decorators =
                Optional.ofNullable(getDecoratorList(context)).orElseGet(List::of);

        GradleInvoker result = invoker;
        for (GradleInvokerDecorator decorator : decorators) {
            result = decorator.decorate(decoratorContext, result);
        }
        return result;
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
