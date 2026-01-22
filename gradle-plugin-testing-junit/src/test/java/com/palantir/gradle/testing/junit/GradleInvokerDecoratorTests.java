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
import com.palantir.gradle.testing.project.RootProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WithArgAddingDecorator(arg = "-PclassValue=classDecorator")
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

    @Test
    @WithArgAddingDecorator2(arg = "help")
    @WithArgAddingDecorator3(arg = "-Pname=hello")
    void multiple_decorators_are_applied(GradleInvoker invoker, RootProject rootProject) {
        rootProject.buildGradle().append("""
            tasks.register("hello") {
                def value = providers.gradleProperty("name")
                def hasClassValue = providers.gradleProperty("classValue")
                doLast {
                    println "Hello from task " + value.get() + " with class decorator " + hasClassValue.get()
                }
            }
            """);

        InvocationResult result = invoker.withArgs("hello").buildsSuccessfully();
        result.assertThat().output().contains("Hello from task hello with class decorator classDecorator");
        result.assertThat().output().contains("Task :help");
    }
}
