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

package com.palantir.gradle.testing.files.gradle.sections;

import com.palantir.gradle.testing.files.gradle.GradleFile;

/**
 * A generic section in Gradle files that works in both top-level and nested contexts.
 * Can be used for sections like plugins, repositories, dependencies, allprojects, subprojects.
 * Uses the canonical ALL_SECTIONS ordering for placement.
 */
public class GenericSection<T extends GradleFile> extends GradleSection<T> {
    public GenericSection(T gradleFile, String blockName) {
        super(gradleFile, blockName);
    }

    public GenericSection(T gradleFile, GradleSection<T> parent, String blockName) {
        super(gradleFile, parent, blockName);
    }
}
