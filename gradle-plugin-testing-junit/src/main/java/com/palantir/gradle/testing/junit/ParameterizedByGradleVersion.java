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
 * Injects a String parameter based on the Gradle version under test.
 *
 * <p>Values are injected positionally into String parameters. Multiple annotations inject values in order.
 *
 * <p>Includes {@code @TestTemplate} - do not combine with {@code @Test}.
 *
 * @see WhenVersion
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ParameterizedByGradleVersions.class)
@TestTemplate
@ExtendWith(ParameterizedByGradleVersionTestTemplateProvider.class)
public @interface ParameterizedByGradleVersion {

    /** Version-specific value conditions. */
    WhenVersion[] value();

    /** Default value(s) when no version condition matches. */
    String[] otherwise() default {};
}
