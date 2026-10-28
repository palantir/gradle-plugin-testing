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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.execution.InvocationResult;
import com.palantir.gradle.testing.junit.TestDecorators.RepeatableWithArgAddingDecorator;
import com.palantir.gradle.testing.junit.TestDecorators.WithArgAddingDecorator;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@RepeatableWithArgAddingDecorator(arg = "-PtopClass=true")
@RepeatableWithArgAddingDecorator(arg = "-PclassValue=classDecorator")
@WithArgAddingDecorator(arg = "-Pfoo=foo")
@GradlePluginTests
class GradleInvokerDecoratorTests {

    @Nested
    class TestWithGradleInvokerSetup {

        @BeforeEach
        void setUp(GradleInvoker invoker, RootProject rootProject) {
            rootProject.buildGradle().append("""
                tasks.register("hello") {
                    def hasClassValue = providers.gradleProperty("classValue")
                    doLast {
                        println "Hello from task with class decorator " + hasClassValue.get()
                    }
                }
                """);
            invoker.withArgs("hello")
                    .buildsSuccessfully()
                    .assertThat()
                    .output()
                    .contains("Hello from task with class decorator classDecorator");
        }

        @Test
        void noop_test() {}
    }

    @BeforeEach
    void setUpWithAGradleInvokerWorks(GradleInvoker _invoker, RootProject rootProject) {
        rootProject.buildGradle().plugins().add("java");
    }

    @Test
    void invoker_works_without_decorators(GradleInvoker invoker, RootProject rootProject) {
        rootProject.buildGradle().append("""
            tasks.register("hello") {
                def hasClassValue = providers.gradleProperty("classValue")
                doLast {
                    println "Hello from task with class decorator " + hasClassValue.get()
                }
            }
            """);

        InvocationResult result = invoker.withArgs("hello").buildsSuccessfully();
        result.assertThat().output().contains("Hello from task with class decorator classDecorator");
    }

    @Test
    @RepeatableWithArgAddingDecorator(arg = "help")
    void decorator_is_applied_via_annotation(GradleInvoker invoker, RootProject rootProject) {
        rootProject.buildGradle().append("""
            tasks.register("hello") {
                doLast {
                    println "Hello from task"
                }
            }
            """);

        InvocationResult result = invoker.withArgs("hello").buildsSuccessfully();
        result.assertThat().output().contains("Hello from task").contains("Task :help");
    }

    @Nested
    @RepeatableWithArgAddingDecorator(arg = "help")
    @RepeatableWithArgAddingDecorator(arg = "-PnestedClass=value")
    @WithArgAddingDecorator(arg = "-Pbar=bar")
    class NestedTest {

        @Test
        @RepeatableWithArgAddingDecorator(arg = "-Pname=hello")
        @RepeatableWithArgAddingDecorator(arg = "-PclassValue2=secondClassDecorator")
        @WithArgAddingDecorator(arg = "-Pbaz=baz")
        void multiple_decorators_are_applied(GradleInvoker invoker, RootProject rootProject) {
            rootProject.buildGradle().append("""
                tasks.register("hello") {
                    def value = providers.gradleProperty("name")
                    def hasClassValue = providers.gradleProperty("classValue")
                    def hasClassValue2 = providers.gradleProperty("classValue2")
                    def foo = providers.gradleProperty("foo")
                    def bar = providers.gradleProperty("bar")
                    def baz = providers.gradleProperty("baz")
                    doLast {
                        println "Hello from task " + value.get() + " with decorator values: " + hasClassValue.get() + " & " + hasClassValue2.get()
                        println " " + foo.get() + " " + bar.get() + " " + baz.get()
                    }
                }
                """);

            InvocationResult result = invoker.withArgs("hello").buildsSuccessfully();
            result.assertThat()
                    .output()
                    .contains("Hello from task hello with decorator values: classDecorator & secondClassDecorator")
                    .contains("foo bar baz")
                    .contains("Task :help");

            assertThatThrownBy(() -> invoker.withArgs("hello").buildsWithFailure())
                    .as("check top-down order (parent classes -> class -> method), ")
                    .hasMessageContaining("hello, -P__TESTING=true, "
                            + // first discovering the `@WithArgAddingDecorator`(most inner class) annotation with the
                            // corresponding args.
                            "-Pfoo=foo, -Pbar=bar, -Pbaz=baz, "
                            + // then the `@RepeatableWithArgAddingDecorator` with the corresponding args.
                            "-PtopClass=true, -PclassValue=classDecorator,"
                            + " help, -PnestedClass=value, -Pname=hello, -PclassValue2=secondClassDecorator,"
                            + " --stacktrace");
        }
    }
}
