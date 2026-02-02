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
import com.palantir.gradle.testing.junit.DisabledConfigurationCache;
import com.palantir.gradle.testing.junit.GradleInvokerDecorator;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.junit.RegistersGradleInvokerDecorator;
import com.palantir.gradle.testing.project.RootProject;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import org.junit.jupiter.api.Test;

@WithWrongDecoratorAnnotation
@GradlePluginTests
public class IncompatibleDecoratorsFixtureTest {

    @Test
    void some_test(GradleInvoker invoker, RootProject rootProject) {
        rootProject.buildGradle().plugins().add("java");
        invoker.withArgs("help").buildsSuccessfully();
    }
}

class SomeDecorator implements GradleInvokerDecorator<DisabledConfigurationCache> {

    @Override
    public GradleInvoker decorate(
            DecoratorContext context, GradleInvoker invoker, List<DisabledConfigurationCache> annotations) {
        throw new RuntimeException("This shouldn't be reachable");
    }
    ;
}

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@RegistersGradleInvokerDecorator(SomeDecorator.class)
@interface WithWrongDecoratorAnnotation {}
