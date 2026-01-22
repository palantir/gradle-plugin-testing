/*
 * (c) Copyright 2026 Palantir Technologies Inc. All rights reserved.
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

public final class DiscoveredDecoratorsByClassStore {
    private static final Namespace NAMESPACE = Namespace.create(DiscoveredDecoratorsByClassStore.class);
    private static final String DISCOVERED_DECORATORS_BY_CLASS = "discoveredDecoratorsByClass";

    /**
     * Stores discovered decorators for a class in the extension context.
     */
    public static void storeDecorators(
            ExtensionContext context, Class<?> clazz, Set<Class<? extends Annotation>> decorators) {
        getDecoratorMap(context).put(clazz, decorators);
    }

    /**
     * Retrieves stored decorators for a class from the extension context.
     */
    public static Set<Class<? extends Annotation>> getStoredDecorators(ExtensionContext context, Class<?> clazz) {
        return getDecoratorMap(context).getOrDefault(clazz, Set.of());
    }

    /**
     * Gets or creates the decorator map from the extension context store.
     */
    @SuppressWarnings("unchecked")
    private static Map<Class<?>, Set<Class<? extends Annotation>>> getDecoratorMap(ExtensionContext context) {
        return (Map<Class<?>, Set<Class<? extends Annotation>>>) context.getStore(NAMESPACE)
                .getOrComputeIfAbsent(DISCOVERED_DECORATORS_BY_CLASS, _key -> new HashMap<>());
    }

    private DiscoveredDecoratorsByClassStore() {}
}
