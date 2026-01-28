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
 * A version condition with associated values for {@link ParameterizedByGradleVersion}.
 *
 * <p>Specify exactly one of: {@code lessThan}, {@code lessThanOrEqualTo}, or {@code equalTo}.
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface WhenVersion {

    /** Matches exactly this version (e.g., "8.14.3"). */
    String equalTo() default "";

    /** Matches versions below this (e.g., "9.0" matches 8.x). */
    String lessThan() default "";

    /** Matches versions up to and including this. */
    String lessThanOrEqualTo() default "";

    /** Values to use when this condition matches. */
    String[] value() default {};
}
