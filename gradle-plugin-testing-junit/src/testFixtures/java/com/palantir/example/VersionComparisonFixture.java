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
 * Fixture class for testing version comparison logic in GradleParameterValues.
 * Not meant to be executed as a test - used only for reflection-based unit tests.
 */
@SuppressWarnings("unused")
public final class VersionComparisonFixture {
    @GradleParameter(name = "value", otherwiseStrings = "new", value = @ForVersion(lessThan = "9.0", strings = "old"))
    public void lessthan_test(String value) {}

    @GradleParameter(
            name = "value",
            otherwiseStrings = "default",
            value = @ForVersion(equalTo = "8.14.3", strings = "special"))
    public void equalto_test(String value) {}

    @GradleParameter(
            name = "value",
            otherwiseStrings = "default",
            value = {
                @ForVersion(lessThan = "9.0", strings = "lessThan"),
                @ForVersion(equalTo = "8.14.3", strings = "equalTo")
            })
    public void overlapping_test(String value) {}

    private VersionComparisonFixture() {}
}
