/*
 * (c) Copyright 2024 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.gradle.plugintesting;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Utility class to maintain and update canonical list versions of gradle to test against.  This helps verify that a
 * plugin is both backwards and forwards compatible.
 */
public final class GradleTestVersions {
    static final String TEST_GRADLE_VERSIONS_SYSTEM_PROPERTY = "TEST_GRADLE_VERSIONS";
    static final List<String> DEFAULT_TEST_GRADLE_VERSIONS = Arrays.asList("7.6.4", "8.8");

    private static final Supplier<List<String>> gradleVersionsSupplier =
            Suppliers.memoize(GradleTestVersions::loadVersions);

    public static List<String> getGradleVersionsForTests() {
        return gradleVersionsSupplier.get();
    }

    private static List<String> loadVersions() {
        //use a set to get rid of duplicates
        Set<String> result = new LinkedHashSet<>();

        if (System.getProperty(TEST_GRADLE_VERSIONS_SYSTEM_PROPERTY) == null) {
            result.addAll(DEFAULT_TEST_GRADLE_VERSIONS);
        } else {
            result.addAll(Arrays.asList(
                    System.getProperty(TEST_GRADLE_VERSIONS_SYSTEM_PROPERTY).split(",")));
        }
        return ImmutableList.copyOf(result);
    }

    private GradleTestVersions() {}
}
