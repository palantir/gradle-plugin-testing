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

package com.palantir.example;

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.junit.DecoratorContext;
import com.palantir.gradle.testing.junit.GradleInvokerDecorator;
import com.palantir.gradle.testing.junit.GradleInvokerDecoratorFactory;
import com.palantir.gradle.testing.junit.RegistersGradleInvokerDecorator;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class Decorators {
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @RegistersGradleInvokerDecorator(DecoratorFactory.class)
    public @interface WithDecorator {}

    public static final class DecoratorFactory implements GradleInvokerDecoratorFactory<WithDecorator> {
        @Override
        public GradleInvokerDecorator create(WithDecorator annotation) {
            return new NoopDecorator();
        }
    }

    public record NoopDecorator() implements GradleInvokerDecorator {
        @Override
        public GradleInvoker decorate(DecoratorContext context, GradleInvoker delegate) {
            return delegate;
        }
    }
}
