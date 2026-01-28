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
 * Injects a String parameter based on the Gradle version under test.
 *
 * <p>Ranges use inclusive lower bounds and exclusive upper bounds. Annotations must cover
 * the entire version space with no gaps (first has no lowerBound, last has no upperBound,
 * each upperBound equals the next lowerBound).
 *
 * <pre>{@code
 * @Test
 * @ParameterizedByGradleVersion(upperBound = "8.0", stringValue = "old")
 * @ParameterizedByGradleVersion(lowerBound = "8.0", stringValue = "new")
 * void test(GradleInvoker invoker, RootProject project, @ParameterInject String behaviour) { }
 * }</pre>
 *
 * @see ParameterInject
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ParameterizedByGradleVersions.class)
public @interface ParameterizedByGradleVersion {

    /** Inclusive lower bound. Empty means minimum */
    String lowerBound() default "";

    /** Exclusive upper bound. Empty means unbounded. */
    String upperBound() default "";

    /** Value to inject when the Gradle version is in this range. */
    String stringValue();
}
