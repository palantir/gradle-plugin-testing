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

import com.palantir.gradle.testing.execution.GradleInvocation;
import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.execution.Options;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.stream.Stream;

public class TestDecorators {

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @RegistersGradleInvokerDecorator(ArgAddingDecoratorFactory.class)
    @interface WithArgAddingDecorator {
        String arg();
    }

    public static final class ArgAddingDecoratorFactory
            implements GradleInvokerDecoratorFactory<WithArgAddingDecorator> {
        @Override
        public GradleInvokerDecorator create(WithArgAddingDecorator annotation) {
            return new ArgAddingDecorator(annotation.arg());
        }
    }

    record ArgAddingDecorator(String argToAdd) implements GradleInvokerDecorator {

        @Override
        public GradleInvoker decorate(DecoratorContext context, GradleInvoker delegate) {
            return new ExtraArgsGradleInvoker(delegate, argToAdd);
        }

        static final class ExtraArgsGradleInvoker extends GradleInvoker {
            private final GradleInvoker delegate;
            private final String argToAdd;

            ExtraArgsGradleInvoker(GradleInvoker delegate, String argToAdd) {
                this.delegate = delegate;
                this.argToAdd = argToAdd;
            }

            @Override
            public GradleInvocation with(Options options) {
                List<String> modifiedArgs = Stream.concat(options.args().stream(), Stream.of(argToAdd))
                        .toList();
                return delegate.with(Options.from(options).args(modifiedArgs).build());
            }
        }
    }

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @RegistersGradleInvokerDecorator(ArgAddingDecoratorFactory2.class)
    @interface WithArgAddingDecorator2 {
        String arg();
    }

    public static final class ArgAddingDecoratorFactory2
            implements GradleInvokerDecoratorFactory<WithArgAddingDecorator2> {
        @Override
        public GradleInvokerDecorator create(WithArgAddingDecorator2 annotation) {
            return new ArgAddingDecorator(annotation.arg());
        }
    }

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @RegistersGradleInvokerDecorator(ArgAddingDecorator3.class)
    @interface WithArgAddingDecorator3 {
        String arg();
    }

    public static final class ArgAddingDecorator3 implements GradleInvokerDecoratorFactory<WithArgAddingDecorator3> {
        @Override
        public GradleInvokerDecorator create(WithArgAddingDecorator3 annotation) {
            return new ArgAddingDecorator(annotation.arg());
        }
    }
}
