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
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Defines a test parameter whose value varies based on the Gradle version being tested.
 *
 * <p>This annotation allows test methods to receive different parameter values depending on which
 * Gradle version the test is running against. Unlike JUnit's {@code @ParameterizedTest} which creates
 * test instances that may be skipped, this annotation only creates test invocations that will actually run.
 *
 * <p>Example usage:
 * <pre>{@code
 * @GradlePluginTests
 * class MyTest {
 *     @GradleParameter(
 *             name = "behavior",
 *             otherwiseStrings = "default",
 *             value = {
 *                 @ForVersion(lessThan = "9.0", strings = {"legacy1", "legacy2"}),
 *                 @ForVersion(equalTo = "8.14.3", strings = "special")
 *             })
 *     void test_behavior(GradleInvoker gradleInvoker, RootProject rootProject, String behavior) {
 *         // Test code using the behavior parameter
 *     }
 * }
 * }</pre>
 *
 * <p><b>Important:</b> All {@link ForVersion} conditions must use the same type (either all strings or all ints),
 * and this type must match the otherwise type used.
 *
 * <p><b>Note:</b> This annotation includes {@code @TestTemplate}, so methods annotated with {@code @GradleParameter}
 * should not also be annotated with {@code @Test}.
 *
 * @see ForVersion
 * @see GradleParameters
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(GradleParameters.class)
@TestTemplate
@ExtendWith(GradleParameterTestTemplateProvider.class)
public @interface GradleParameter {

    /**
     * The name of the parameter. This must match the name of a method parameter.
     *
     * @return the parameter name
     */
    String name();

    /**
     * Version-specific value conditions.
     *
     * @return array of version conditions with their associated values
     */
    ForVersion[] value();

    /**
     * Default string values when no version condition matches.
     * Mutually exclusive with {@link #otherwiseInt()}.
     *
     * @return array of default string values
     */
    String[] otherwiseStrings() default {};

    /**
     * Default integer values when no version condition matches.
     * Mutually exclusive with {@link #otherwiseStrings()}.
     *
     * @return array of default integer values
     */
    int[] otherwiseInt() default {};
}
