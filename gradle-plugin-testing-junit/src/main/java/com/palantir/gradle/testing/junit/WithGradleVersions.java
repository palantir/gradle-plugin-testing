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
 * Annotation for adding additional Gradle versions to individual test classes or methods.
 *
 * <p><b>Important:</b> This annotation is intended for exceptional cases where a specific test or test class
 * needs to run against additional Gradle versions beyond the globally configured versions. For configuring
 * Gradle versions across your entire test suite, prefer setting versions in the
 * {@code gradle/gradle-test-versions.yml} file
 *
 * <p>The versions specified in this annotation will be merged with the versions configured in the yml file.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface WithGradleVersions {

    /**
     * The additional Gradle versions to test against.
     * @return an array of Gradle version strings (e.g., "7.6.5", "8.0")
     */
    String[] value();

    /**
     * Optional reason explaining why these additional Gradle versions are needed.
     * @return the reason for requiring these specific versions
     */
    String reason() default "";
}
