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

package com.palantir.example;

import com.palantir.gradle.testing.junit.ForVersion;
import com.palantir.gradle.testing.junit.GradleParameter;

/**
 * Fixture class for testing type mismatch validation in GradleParameterValues.
 * Not meant to be executed as a test - used only for reflection-based unit tests.
 */
@SuppressWarnings("unused")
public final class TypeMismatchFixture {
    @GradleParameter(name = "value", otherwiseStrings = "default", value = @ForVersion(lessThan = "9.0", ints = 1))
    public void strings_otherwise_but_ints_forversion(String value) {}

    @GradleParameter(name = "value", otherwiseInt = 1, value = @ForVersion(lessThan = "9.0", strings = "wrong"))
    public void ints_otherwise_but_strings_forversion(int value) {}

    @GradleParameter(
            name = "value",
            otherwiseStrings = "default",
            value = @ForVersion(lessThan = "9.0", strings = "a", ints = 1))
    public void forversion_both_types(String value) {}

    @GradleParameter(
            name = "value",
            otherwiseStrings = "default",
            otherwiseInt = 1,
            value = @ForVersion(lessThan = "9.0", strings = "a"))
    public void otherwise_both_types(String value) {}

    @GradleParameter(name = "value", otherwiseStrings = "default", value = @ForVersion(strings = "a"))
    public void forversion_no_condition(String value) {}

    @GradleParameter(
            name = "value",
            otherwiseStrings = "default",
            value = @ForVersion(lessThan = "9.0", equalTo = "8.0", strings = "a"))
    public void forversion_both_conditions(String value) {}

    @GradleParameter(name = "value", value = @ForVersion(lessThan = "9.0", strings = "a"))
    public void no_otherwise_no_match(String value) {}

    private TypeMismatchFixture() {}
}
