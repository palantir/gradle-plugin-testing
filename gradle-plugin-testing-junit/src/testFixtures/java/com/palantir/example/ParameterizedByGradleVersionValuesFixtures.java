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

import com.palantir.gradle.testing.junit.InjectByGradleVersion;
import com.palantir.gradle.testing.junit.ParameterizedByGradleVersion;
import com.palantir.gradle.testing.junit.ParameterizedByGradleVersion.WhenVersion;

public final class ParameterizedByGradleVersionValuesFixtures {

    public static class ValidFixtures {

        @ParameterizedByGradleVersion(
                when = @WhenVersion(lessThan = "8.0", stringValue = "old"),
                otherwiseString = "new")
        public void singleCondition(@InjectByGradleVersion String behaviour) {}

        @ParameterizedByGradleVersion(
                when = {
                    @WhenVersion(lessThan = "8.0", stringValue = "less than 8"),
                    @WhenVersion(lessThan = "9.0", stringValue = "8.x")
                },
                otherwiseString = "9 and up")
        public void twoConditions(@InjectByGradleVersion String behaviour) {}

        @ParameterizedByGradleVersion(
                name = "first",
                when = @WhenVersion(lessThan = "8.0", stringValue = "old"),
                otherwiseString = "new")
        @ParameterizedByGradleVersion(
                name = "second",
                when = @WhenVersion(lessThan = "9.0", stringValue = "before 9"),
                otherwiseString = "after 9")
        public void twoAnnotations(@InjectByGradleVersion String first, @InjectByGradleVersion String second) {}

        public void noAnnotation() {}
    }

    public static class InvalidFixtures {

        @ParameterizedByGradleVersion(
                when = {
                    @WhenVersion(lessThan = "9.0", stringValue = "a"),
                    @WhenVersion(lessThan = "8.0", stringValue = "b")
                },
                otherwiseString = "default")
        public void descendingOrder(@InjectByGradleVersion String behaviour) {}

        @ParameterizedByGradleVersion(
                when = {
                    @WhenVersion(lessThan = "8.0", stringValue = "a"),
                    @WhenVersion(lessThan = "8.0", stringValue = "b")
                },
                otherwiseString = "default")
        public void duplicateVersions(@InjectByGradleVersion String behaviour) {}

        @ParameterizedByGradleVersion(
                when = {
                    @WhenVersion(lessThan = "7.0", stringValue = "a"),
                    @WhenVersion(lessThan = "9.0", stringValue = "b"),
                    @WhenVersion(lessThan = "8.0", stringValue = "c")
                },
                otherwiseString = "default")
        public void outOfOrderMiddle(@InjectByGradleVersion String behaviour) {}

        @ParameterizedByGradleVersion(
                name = "behaviour",
                when = @WhenVersion(lessThan = "8.0", stringValue = "old"),
                otherwiseString = "first")
        @ParameterizedByGradleVersion(
                name = "behaviour",
                when = @WhenVersion(lessThan = "9.0", stringValue = "also old"),
                otherwiseString = "second")
        public void duplicateNames(@InjectByGradleVersion String behaviour, @InjectByGradleVersion String other) {}

        @ParameterizedByGradleVersion(
                name = "first",
                when = @WhenVersion(lessThan = "8.0", stringValue = "old"),
                otherwiseString = "new")
        @ParameterizedByGradleVersion(
                when = @WhenVersion(lessThan = "9.0", stringValue = "before 9"),
                otherwiseString = "after 9")
        public void missingNameOnSecond(@InjectByGradleVersion String first, @InjectByGradleVersion String second) {}

        @ParameterizedByGradleVersion(
                when = @WhenVersion(lessThan = "8.0", stringValue = "old"),
                otherwiseString = "new")
        public void missingInjectParameter() {}

        @ParameterizedByGradleVersion(
                name = "first",
                when = @WhenVersion(lessThan = "8.0", stringValue = "old"),
                otherwiseString = "new")
        @ParameterizedByGradleVersion(
                name = "second",
                when = @WhenVersion(lessThan = "9.0", stringValue = "before 9"),
                otherwiseString = "after 9")
        public void twoAnnotationsButOnlyOneParameter(@InjectByGradleVersion String first) {}

        @ParameterizedByGradleVersion(
                when = @WhenVersion(lessThan = "8.0", stringValue = "old"),
                otherwiseString = "new")
        public void extraInjectParameter(@InjectByGradleVersion String first, @InjectByGradleVersion String second) {}

        @ParameterizedByGradleVersion(
                name = "wrongName",
                when = @WhenVersion(lessThan = "8.0", stringValue = "old"),
                otherwiseString = "new")
        public void mismatchedAnnotationName(@InjectByGradleVersion String param) {}

        @ParameterizedByGradleVersion(
                name = "first",
                when = @WhenVersion(lessThan = "8.0", stringValue = "old"),
                otherwiseString = "new")
        public void extraParameterWithWrongName(
                @InjectByGradleVersion String first, @InjectByGradleVersion String extra) {}
    }

    private ParameterizedByGradleVersionValuesFixtures() {}
}
