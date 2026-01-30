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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects a parameter based on the Gradle version under test.
 *
 * <p>Simple usage (single parameter):
 * <pre>{@code
 * @ParameterizedByGradleVersion(
 *     when = @WhenVersion(lessThan = "8.0", stringValue = "old"),
 *     otherwiseString = "new")
 * void test(GradleInvoker invoker, RootProject project, @InjectByGradleVersion String behaviour) { }
 * }</pre>
 *
 * <p>Multiple parameters (name must match the method parameter name):
 * <pre>{@code
 * @ParameterizedByGradleVersion(
 *     name = "style",
 *     when = @WhenVersion(lessThan = "8.0", stringValue = "old"),
 *     otherwiseString = "new")
 * @ParameterizedByGradleVersion(
 *     name = "format",
 *     when = @WhenVersion(lessThan = "9.0", stringValue = "classic"),
 *     otherwiseString = "modern")
 * void test(GradleInvoker invoker, @InjectByGradleVersion String style, @InjectByGradleVersion String format) { }
 * }</pre>
 *
 * <p>Conditions must be ordered by ascending version (lowest first). The following will fail at test runtime:
 * <pre>{@code
 * @ParameterizedByGradleVersion(
 *     when = {
 *         @WhenVersion(lessThan = "9.0", stringValue = "a"),
 *         @WhenVersion(lessThan = "8.0", stringValue = "b")
 *     },
 *     otherwiseString = "c")
 * }</pre>
 *
 * @see InjectByGradleVersion
 * @see WhenVersion
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ParameterizedByGradleVersions.class)
public @interface ParameterizedByGradleVersion {

    /** Parameter name. Required when multiple annotations are present, optional for single annotation. */
    String name() default "";

    WhenVersion[] when() default {};

    String otherwiseString();

    /** Version condition. Matches when Gradle version is less than threshold. */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    @interface WhenVersion {

        String lessThan();

        String stringValue();
    }
}
