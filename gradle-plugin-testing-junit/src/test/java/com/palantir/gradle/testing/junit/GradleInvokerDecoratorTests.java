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
import com.palantir.gradle.testing.execution.InvocationResult;
import com.palantir.gradle.testing.junit.TestDecorators.WithArgAddingDecorator;
import com.palantir.gradle.testing.junit.TestDecorators.WithArgAddingDecorator2;
import com.palantir.gradle.testing.junit.TestDecorators.WithArgAddingDecorator3;
import com.palantir.gradle.testing.junit.TestDecorators.WithCountingDecorator;
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@WithArgAddingDecorator(arg = "-PclassValue=classDecorator")
@GradlePluginTests
@DisabledConfigurationCache("Testing decorator mechanism without config cache complexity")
class GradleInvokerDecoratorTests {

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
    @WithArgAddingDecorator2(arg = "help")
    void decorator_is_applied_via_annotation(GradleInvoker invoker, RootProject rootProject) {
        rootProject.buildGradle().append("""
            tasks.register("hello") {
                doLast {
                    println "Hello from task"
                }
            }
            """);

        InvocationResult result = invoker.withArgs("hello").buildsSuccessfully();
        result.assertThat().output().contains("Hello from task");
        result.assertThat().output().contains("Task :help");
    }

    @Nested
    @WithArgAddingDecorator2(arg = "help")
    class NestedTest {

        @Test
        @WithArgAddingDecorator3(arg = "-Pname=hello")
        @WithArgAddingDecorator(arg = "-PclassValue2=secondClassDecorator")
        void multiple_decorators_are_applied(GradleInvoker invoker, RootProject rootProject) {
            rootProject.buildGradle().append("""
                tasks.register("hello") {
                    def value = providers.gradleProperty("name")
                    def hasClassValue = providers.gradleProperty("classValue")
                    def hasClassValue2 = providers.gradleProperty("classValue2")
                    doLast {
                        println "Hello from task " + value.get() + " with decorator values: " + hasClassValue.get() + " & " + hasClassValue2.get()
                    }
                }
                """);

            InvocationResult result = invoker.withArgs("hello").buildsSuccessfully();
            result.assertThat()
                    .output()
                    .contains("Hello from task hello with decorator values: classDecorator & secondClassDecorator");
            result.assertThat().output().contains("Task :help");
        }
    }

    @Nested
    @WithCountingDecorator
    public class DuplicateDecoratorTests {

        @BeforeEach
        void setup(RootProject rootProject) {
            rootProject.buildGradle().append("""
                tasks.register("decoratorCount") {
                    doLast {
                        def count = project.properties.count { k, v -> k.toString().startsWith("decoratorId") }
                        println "Decorator count: ${count}"
                    }
                }
                """);
        }

        @Test
        @WithCountingDecorator
        void duplicate_decorator_on_test_method(GradleInvoker invoker) {
            checkOnlyFirstDecoratorRegistered(invoker);
        }

        @Nested
        class OtherTestClass {

            @WithCountingDecorator
            @Test
            void nested_test_with_duplicated_decorator(GradleInvoker invoker) {
                checkOnlyFirstDecoratorRegistered(invoker);
            }
        }

        @WithCountingDecorator
        @Nested
        class TestClassWithDuplicateDecorator {

            @Test
            void simple_test(GradleInvoker invoker) {
                checkOnlyFirstDecoratorRegistered(invoker);
            }
        }
    }

    private static void checkOnlyFirstDecoratorRegistered(GradleInvoker invoker) {
        invoker.withArgs("decoratorCount")
                .buildsSuccessfully()
                .assertThat()
                .output()
                .contains("Decorator count: 1");
    }
}
