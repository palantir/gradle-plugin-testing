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
 * Injects a String parameter based on the Gradle version under test.
 *
 * <pre>{@code
 * @ParameterizedByGradleVersion(
 *     name = "behaviour",
 *     otherwiseString = "new",
 *     when = @WhenVersion(lessThan = "8.0", stringValue = "old"))
 * void test(GradleInvoker invoker, RootProject project, String behaviour) { }
 * }</pre>
 *
 * <p>Conditions must be ordered by ascending version (lowest first).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ParameterizedByGradleVersion {

    String name();

    String otherwiseString();

    WhenVersion[] when() default {};
}
