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

import com.palantir.gradle.testing.execution.GradleInvocation;
import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.execution.InvocationResult;
import com.palantir.gradle.testing.project.RootProject;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@GradlePluginTests
@DisabledConfigurationCache("Testing decorator mechanism without config cache complexity")
class GradleInvokerDecoratorTests {

    @BeforeEach
    void setUp(RootProject rootProject) {
        rootProject.buildGradle().plugins().add("java");
    }

    @Test
    void invoker_works_without_decorators(GradleInvoker invoker, RootProject rootProject) {
        rootProject.buildGradle().append("""
            tasks.register("hello") {
                doLast {
                    println "Hello from task"
                }
            }
            """);

        InvocationResult result = invoker.withArgs("hello").buildsSuccessfully();
        result.assertThat().output().contains("Hello from task");
    }

    @Test
    @WithArgAddingDecorator(arg = "--info")
    void decorator_is_applied_via_annotation(GradleInvoker invoker, RootProject rootProject) {
        rootProject.buildGradle().append("""
            tasks.register("hello") {
                doLast {
                    println "Hello from task"
                }
            }
            """);

        // The decorator adds --info flag, so we should see verbose output
        InvocationResult result = invoker.withArgs("hello").buildsSuccessfully();
        result.assertThat().output().contains("Hello from task");
        // With --info, we see "Task :hello" status line
        result.assertThat().output().contains("Task :hello");
    }

    @Test
    @WithArgAddingDecorator(arg = "--info")
    @WithArgAddingDecorator2(arg = "--stacktrace")
    void multiple_decorators_are_applied(GradleInvoker invoker, RootProject rootProject) {
        rootProject.buildGradle().append("""
            tasks.register("hello") {
                doLast {
                    println "Hello from task"
                }
            }
            """);

        // Both decorators should add their args
        InvocationResult result = invoker.withArgs("hello").buildsSuccessfully();
        result.assertThat().output().contains("Hello from task");
        // --info adds verbose output
        result.assertThat().output().contains("Task :hello");
    }

    @Test
    @WithContextAccessingDecorator
    void decorator_has_access_to_context(GradleInvoker invoker, RootProject rootProject) {
        rootProject.buildGradle().append("""
            tasks.register("hello") {
                doLast {
                    println "Hello from task"
                }
            }
            """);

        // The decorator adds a property based on the context
        InvocationResult result = invoker.withArgs("hello").buildsSuccessfully();
        result.assertThat().output().contains("Hello from task");
    }

    // --- Test Decorators ---

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @RegistersGradleInvokerDecorator(ArgAddingDecoratorFactory.class)
    @interface WithArgAddingDecorator {
        String arg();
    }

    public static class ArgAddingDecoratorFactory implements GradleInvokerDecoratorFactory<WithArgAddingDecorator> {
        @Override
        public GradleInvokerDecorator create(WithArgAddingDecorator annotation) {
            return new ArgAddingDecorator(annotation.arg());
        }
    }

    static class ArgAddingDecorator implements GradleInvokerDecorator {
        private final String argToAdd;

        ArgAddingDecorator(String argToAdd) {
            this.argToAdd = argToAdd;
        }

        @Override
        public GradleInvoker decorate(DecoratorContext context, GradleInvoker delegate) {
            return new GradleInvoker() {
                @Override
                public GradleInvocation withArgs(String... args) {
                    String[] modifiedArgs = new String[args.length + 1];
                    System.arraycopy(args, 0, modifiedArgs, 0, args.length);
                    modifiedArgs[args.length] = argToAdd;
                    return delegate.withArgs(modifiedArgs);
                }
            };
        }
    }

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @RegistersGradleInvokerDecorator(ArgAddingDecoratorFactory2.class)
    @interface WithArgAddingDecorator2 {
        String arg();
    }

    public static class ArgAddingDecoratorFactory2 implements GradleInvokerDecoratorFactory<WithArgAddingDecorator2> {
        @Override
        public GradleInvokerDecorator create(WithArgAddingDecorator2 annotation) {
            return new ArgAddingDecorator(annotation.arg());
        }
    }

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @RegistersGradleInvokerDecorator(ContextAccessingDecoratorFactory.class)
    @interface WithContextAccessingDecorator {}

    public static class ContextAccessingDecoratorFactory
            implements GradleInvokerDecoratorFactory<WithContextAccessingDecorator> {
        @Override
        public GradleInvokerDecorator create(WithContextAccessingDecorator _annotation) {
            return new ContextAccessingDecorator();
        }
    }

    static class ContextAccessingDecorator implements GradleInvokerDecorator {
        @Override
        public GradleInvoker decorate(DecoratorContext context, GradleInvoker delegate) {
            // Verify we have access to context properties
            if (context.rootProjectDir() == null) {
                throw new IllegalStateException("rootProjectDir should not be null");
            }
            if (context.gradleVersion() == null) {
                throw new IllegalStateException("gradleVersion should not be null");
            }
            if (context.extensionContext() == null) {
                throw new IllegalStateException("extensionContext should not be null");
            }
            return delegate;
        }
    }
}
