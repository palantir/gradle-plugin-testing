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
 * Annotation for filtering Gradle versions to only run versions less than the specified version.
 *
 * <p>This annotation filters the available versions to only include those strictly less than
 * the specified version (exclusive). For example, {@code @WithGradleVersionsLessThan("8.0")}
 * will run on 7.6.5 but not on 8.0 or 8.1.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface WithGradleVersionsLessThan {

    /**
     * The upper bound Gradle version (exclusive).
     * @return a Gradle version string (e.g., "8.0")
     */
    String value();

    /**
     * Optional reason explaining why this version constraint is applied.
     * @return the reason for this version constraint
     */
    String reason() default "";
}
