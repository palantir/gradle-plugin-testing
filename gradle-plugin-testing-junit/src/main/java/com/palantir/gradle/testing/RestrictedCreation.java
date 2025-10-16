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

package com.palantir.gradle.testing;

public final class RestrictedCreation {
    public static final String EXPLANATION =
            "Use junit injected objects like GradleInvoker, RootProject or SubProject etc instead rather than "
                    + "creating these manually. If there really isn't some way to do what you need through the "
                    + "injected types, please contribute to palantir/gradle-plugin-testing rather than manually "
                    + "working around the lack of functionality";

    public static final String ALLOWED_ON_PATH = ".*/com/palantir/gradle/testing/.*";

    private RestrictedCreation() {}
}
