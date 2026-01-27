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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies values for a specific Gradle version condition within a {@link GradleParameter}.
 *
 * <p>Version conditions can be specified using either {@code lessThan} or {@code equalTo}, but not both.
 *
 * <p>Exactly one of the value arrays must be non-empty and match the type used in the containing
 * {@link GradleParameter}'s otherwise value.
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface ForVersion {

    /**
     * The Gradle version that must be matched exactly for this condition to apply.
     * Mutually exclusive with {@link #lessThan()}.
     *
     * @return the exact version to match (e.g., "8.14.3"), or empty string if not used
     */
    String equalTo() default "";

    /**
     * The Gradle version that the current version must be less than for this condition to apply.
     * Mutually exclusive with {@link #equalTo()}.
     *
     * @return the version to compare against (e.g., "9.3.0"), or empty string if not used
     */
    String lessThan() default "";

    /**
     * String values to use when this version condition matches.
     *
     * @return array of string values
     */
    String[] strings() default {};

    /**
     * Integer values to use when this version condition matches.
     *
     * @return array of integer values
     */
    int[] ints() default {};

    /**
     * Long values to use when this version condition matches.
     *
     * @return array of long values
     */
    long[] longs() default {};

    /**
     * Double values to use when this version condition matches.
     *
     * @return array of double values
     */
    double[] doubles() default {};

    /**
     * Boolean values to use when this version condition matches.
     *
     * @return array of boolean values
     */
    boolean[] booleans() default {};
}
