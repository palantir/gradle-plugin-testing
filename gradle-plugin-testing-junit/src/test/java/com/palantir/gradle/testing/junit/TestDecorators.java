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

import com.palantir.gradle.testing.execution.GradleInvoker;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class TestDecorators {

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface WithArgAddingDecorators {
        RepeatableWithArgAddingDecorator[] value();
    }

    @Repeatable(WithArgAddingDecorators.class)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @RegistersGradleInvokerDecorator(RepeatableArgAddingDecorator.class)
    public @interface RepeatableWithArgAddingDecorator {
        String arg();
    }

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @RegistersGradleInvokerDecorator(ArgAddingDecorator.class)
    public @interface WithArgAddingDecorator {
        String arg();
    }

    public static final class RepeatableArgAddingDecorator
            implements GradleInvokerDecorator<RepeatableWithArgAddingDecorator> {

        @Override
        public GradleInvoker decorate(
                DecoratorContext context, GradleInvoker invoker, List<RepeatableWithArgAddingDecorator> annotations) {
            return args -> {
                String[] modifiedArgs = Stream.concat(
                                Arrays.stream(args),
                                annotations.stream().map(RepeatableWithArgAddingDecorator::arg).toList().stream())
                        .toArray(String[]::new);
                return invoker.withArgs(modifiedArgs);
            };
        }
    }

    public static final class ArgAddingDecorator implements GradleInvokerDecorator<WithArgAddingDecorator> {

        @Override
        public GradleInvoker decorate(
                DecoratorContext context, GradleInvoker invoker, List<WithArgAddingDecorator> annotations) {
            return args -> {
                String[] modifiedArgs = Stream.concat(
                                Arrays.stream(args),
                                annotations.stream().map(WithArgAddingDecorator::arg).toList().stream())
                        .toArray(String[]::new);
                return invoker.withArgs(modifiedArgs);
            };
        }
    }
}
