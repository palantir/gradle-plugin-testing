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
 * Annotation that can be used alongside {@link GradlePluginTests} to disable configuration cache testing.
 * When applied, all Gradle tasks executed via {@link com.palantir.gradle.testing.execution.GradleInvoker} will run the
 * {@link com.palantir.gradle.testing.execution.DefaultGradleInvoker}. This is useful for tests that are incompatible
 * with the configuration cache. This annotation can be applied to a class or to individual test methods.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@RegistersGradleInvokerDecorator(ConfigurationCacheDecorator.class)
public @interface DisabledConfigurationCache {

    /**
     * The reason why the configuration cache is disabled.
     * @return the reason as a string
     */
    String value() default "";
}
