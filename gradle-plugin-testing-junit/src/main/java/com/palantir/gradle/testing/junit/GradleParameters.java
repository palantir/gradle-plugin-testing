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
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Container annotation for multiple {@link GradleParameter} annotations.
 *
 * <p>When multiple {@link GradleParameter} annotations are applied to a method, the test framework
 * generates test invocations for the Cartesian product of all parameter values. For example, if one
 * parameter has 2 values and another has 3 values, 6 test invocations will be generated (for each
 * Gradle version).
 *
 * <p>This annotation is automatically applied when multiple {@link GradleParameter} annotations
 * are present on a method.
 *
 * @see GradleParameter
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@TestTemplate
@ExtendWith(GradleParameterTestTemplateProvider.class)
public @interface GradleParameters {

    /**
     * The array of {@link GradleParameter} annotations.
     *
     * @return the contained GradleParameter annotations
     */
    GradleParameter[] value();
}
