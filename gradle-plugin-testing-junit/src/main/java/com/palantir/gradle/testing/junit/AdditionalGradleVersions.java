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
 * Annotation that can be used to add additional Gradle versions to test against.
 * The versions specified in this annotation will be merged with the versions configured via the
 * {@code com.palantir.gradle.testing.gradle_versions_to_test} configuration parameter.
 * This annotation can be applied to a test class or individual test methods.
 *
 * <p>When applied to both a class and a method, the versions are combined (base + class + method versions).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AdditionalGradleVersions {

    /**
     * The additional Gradle versions to test against.
     * @return an array of Gradle version strings (e.g., "7.6.5", "8.0")
     */
    String[] value();
}
