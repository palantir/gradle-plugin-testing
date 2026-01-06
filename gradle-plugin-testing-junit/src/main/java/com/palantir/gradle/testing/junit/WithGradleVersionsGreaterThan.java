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
 * Annotation for filtering Gradle versions to only run versions greater than the specified version.
 *
 * <p>This annotation filters the available versions to only include those strictly greater than
 * the specified version (exclusive). For example, {@code @WithGradleVersionsGreaterThan("7.6")}
 * will run on 8.0 but not on 7.6 or 7.5.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface WithGradleVersionsGreaterThan {

    /**
     * The lower bound Gradle version (exclusive).
     * @return a Gradle version string (e.g., "7.6")
     */
    String value();

    /**
     * Optional reason explaining why this version constraint is applied.
     * @return the reason for this version constraint
     */
    String reason() default "";
}
