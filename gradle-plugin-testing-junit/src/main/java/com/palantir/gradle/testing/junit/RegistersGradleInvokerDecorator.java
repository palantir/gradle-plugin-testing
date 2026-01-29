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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation that marks an annotation as registering a {@link GradleInvokerDecorator}.
 *
 * <p>When a test class or method is annotated with an annotation that has this meta-annotation,
 * the framework will automatically:
 * <ol>
 *   <li>Instantiate the specified {@link GradleInvokerDecoratorFactory}</li>
 *   <li>Call {@link GradleInvokerDecoratorFactory#create} with all matching annotation instance</li>
 *   <li>Register the resulting decorator</li>
 * </ol>
 *
 * <p>Example usage:
 * <pre>{@code
 * @Target({ElementType.TYPE, ElementType.METHOD})
 * @Retention(RetentionPolicy.RUNTIME)
 * @RegistersGradleInvokerDecorator(MyDecoratorFactory.class)
 * public @interface WithMyFeature {
 *     // Optional configuration attributes
 * }
 * }</pre>
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RegistersGradleInvokerDecorator {

    /**
     * The factory class that creates the decorator.
     * Must have a public no-argument constructor.
     */
    Class<? extends GradleInvokerDecoratorFactory> value();
}
