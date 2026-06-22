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
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * Meta-annotation that marks an annotation as registering a {@link GradleInvokerDecorator}.
 *
 * <p>When a test class or method is annotated with an annotation that has this meta-annotation,
 * the framework will automatically:
 * <ol>
 *   <li>Instantiate the specified {@link GradleInvokerDecorator}</li>
 *   <li>Call {@link GradleInvokerDecorator#decorate(DecoratorContext, GradleInvoker, List)} with all matching annotation type instances</li>
 * </ol>
 *
 * <p>Example usage:
 * <pre>{@code
 * @Target({ElementType.TYPE, ElementType.METHOD})
 * @Retention(RetentionPolicy.RUNTIME)
 * @RegistersGradleInvokerDecorator(MyDecorator.class)
 * public @interface WithMyFeature {
 *     // Optional configuration attributes
 * }
 * }</pre>
 */
@Repeatable(RegistersGradleInvokerDecorators.class)
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RegistersGradleInvokerDecorator {

    /**
     * The decorator class that decorates the gradle invoker.
     */
    Class<? extends GradleInvokerDecorator> value();
}
