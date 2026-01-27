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
 * Gradle version the test is running against. Similar to JUnit's {@code @ParameterizedTest}, but
 * the parameter values are selected based on Gradle version conditions.
 *
 * <p><b>Note:</b> This annotation includes {@code @TestTemplate}, so methods annotated with
 * {@code @ParameterizedByGradleVersion} should not also be annotated with {@code @Test}.
 *
 * @see WhenVersion
 * @see ParameterizedByGradleVersions
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ParameterizedByGradleVersions.class)
@TestTemplate
@ExtendWith(ParameterizedByGradleVersionTestTemplateProvider.class)
public @interface ParameterizedByGradleVersion {

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
    WhenVersion[] value();

    /**
     * Default string values when no version condition matches.
     * Mutually exclusive with other otherwise* attributes.
     *
     * @return array of default string values
     */
    String[] otherwiseStrings() default {};

    /**
     * Default integer values when no version condition matches.
     * Mutually exclusive with other otherwise* attributes.
     *
     * @return array of default integer values
     */
    int[] otherwiseInt() default {};

    /**
     * Default long values when no version condition matches.
     * Mutually exclusive with other otherwise* attributes.
     *
     * @return array of default long values
     */
    long[] otherwiseLong() default {};

    /**
     * Default double values when no version condition matches.
     * Mutually exclusive with other otherwise* attributes.
     *
     * @return array of default double values
     */
    double[] otherwiseDouble() default {};

    /**
     * Default boolean values when no version condition matches.
     * Mutually exclusive with other otherwise* attributes.
     *
     * @return array of default boolean values
     */
    boolean[] otherwiseBoolean() default {};
}
